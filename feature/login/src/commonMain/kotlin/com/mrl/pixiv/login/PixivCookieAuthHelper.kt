package com.mrl.pixiv.login

import com.mrl.pixiv.common.network.CookieAuthClient
import com.mrl.pixiv.common.repository.AuthManager
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.get
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.ByteString.Companion.toByteString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import kotlin.io.encoding.Base64
import kotlin.random.Random
import kotlin.time.Clock

// ─── 常量 ─────────────────────────────────────────────────────────────────────

private const val REDIRECT_LOGIN_URL = "https://app-api.pixiv.net/web/v1/login"
private const val ORIGIN_URL = "https://accounts.pixiv.net"
private const val LOGIN_URL = "https://accounts.pixiv.net/login"
private const val POST_SELECTED_URL = "https://accounts.pixiv.net/account-selected"
private const val POST_REDIRECT_URL = "https://accounts.pixiv.net/post-redirect"
private const val VERIFIER_CHARS =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
private const val VERIFIER_LENGTH = 128

// ─── Helper ───────────────────────────────────────────────────────────────────

/**
 * 通过 Pixiv 网页端 Cookie（含 PHPSESSID）换取 App API 的 OAuth Token。
 *
 * 用法：
 * ```kotlin
 * val helper = PixivCookieAuthHelper()
 * helper.login(cookies)
 * helper.close()
 * ```
 *
 * @param engineConfig 可选，对底层 [HttpClient] 做额外配置（如代理）
 */
class PixivCookieAuthHelper(
    engineConfig: HttpClientConfig<*>.() -> Unit = {}
) : AutoCloseable, KoinComponent {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val cookieStorage = AcceptAllCookiesStorage()

    private val client by inject<HttpClient>(named<CookieAuthClient>()) {
        parametersOf(json, cookieStorage, engineConfig)
    }

    // ── 公开入口 ──────────────────────────────────────────────────────────────

    /**
     * 使用网页端 Cookie 列表换取 OAuth Token。
     *
     * @param cookies 从浏览器导出的 Cookie 列表，**必须包含 PHPSESSID**。
     *                可通过浏览器扩展（EditThisCookie 等）导出后映射为 [Cookie] 对象。
     * @throws IllegalStateException 若 PHPSESSID 缺失或账号未登录
     * @throws IllegalStateException 若任一重定向跳转失败
     */
    suspend fun login(cookies: List<Cookie>) {
        // 1. 生成 PKCE 参数
        val (verifier, codeChallenge) = generatePkce()

        // 2. 构造带 PKCE 参数的登录入口 URL
        val redirectUrl = URLBuilder(REDIRECT_LOGIN_URL).apply {
            parameters.append("code_challenge", codeChallenge)
            parameters.append("code_challenge_method", "S256")
            parameters.append("client", "pixiv-android")
        }.build()

        // 3. 注入 Cookie
        injectCookies(cookies)

        // 4. 通过 cookie 换取 authorization code
        val code = fetchAuthorizationCode(redirectUrl)

        // 5. 用 code + verifier 换取最终 Token
        AuthManager.login(code, verifier)
    }

    override fun close() = client.close()

    // ── PKCE ──────────────────────────────────────────────────────────────────

    private data class Pkce(val verifier: String, val challenge: String)

    private fun generatePkce(): Pkce {
        val random = Random(Clock.System.now().epochSeconds)
        val verifier = buildString(VERIFIER_LENGTH) {
            repeat(VERIFIER_LENGTH) { append(VERIFIER_CHARS[random.nextInt(VERIFIER_CHARS.length)]) }
        }
        val challenge = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(
            verifier.encodeToByteArray().toByteString().sha256().toByteArray()
        )
        return Pkce(verifier, challenge)
    }

    // ── Cookie 注入 ────────────────────────────────────────────────────────────

    private suspend fun injectCookies(cookies: List<Cookie>) {
        val origin = Url(ORIGIN_URL)
        for (cookie in cookies) {
            cookieStorage.addCookie(origin, cookie)
        }
        requireNotNull(cookieStorage.get(origin)["PHPSESSID"]) {
            "Cookie 中未找到 PHPSESSID，请确认已提供正确的 Pixiv 登录 Cookie"
        }
    }

    // ── 核心流程 ───────────────────────────────────────────────────────────────

    /**
     * 完整执行从 redirect URL → pixiv://...?code=xxx 的跳转链，返回 Authorization Code
     */
    private suspend fun fetchAuthorizationCode(redirectUrl: Url): String {
        // Step 1: 访问登录入口，拿到账号元数据
        val initialLoginPage: HttpResponse = client.get(redirectUrl) {
            header(HttpHeaders.Referrer, LOGIN_URL)
        }
        // Step 2: 拿到 https://accounts.pixiv.net/login?prompt=select_account URL
        val loginSelectAccountUrl = requireNotNull(initialLoginPage.location()) {
            "未能从登录入口获取跳转地址（期望跳转至账号选择页）"
        }
        // Step 3: 拿到Html，解析出 continueUrl 和 tt（CSRF token）
        val realLoginPage = client.get(loginSelectAccountUrl) {
            header(HttpHeaders.Referrer, LOGIN_URL)
        }
        val (continueUrl, tt) = parseHtmlAccount(realLoginPage.bodyAsText())
        // Step 4: POST account-selected，提交账号确认 + CSRF token
        val accountSelected: HttpResponse = client.submitForm(
            url = POST_SELECTED_URL,
            formParameters = Parameters.build {
                append("return_to", continueUrl)
                append("tt", tt)
            }
        ) {
            header(HttpHeaders.Origin, ORIGIN_URL)
            header(HttpHeaders.Referrer, loginSelectAccountUrl)
        }
        // Step 5: 拿到 post-redirect的 URL
        val postRedirectLink = requireNotNull(accountSelected.location()) {
            "未能从 account-selected 获取跳转地址（期望跳转至 $POST_REDIRECT_URL）"
        }
        // Step 6: 进入 redirect() 逻辑 —— 处理 POST_REDIRECT_URL 链路
        return resolvePostRedirect(postRedirectLink)
    }

    /**
     * 从 POST_REDIRECT_URL → START_URL → OAUTH_AUTHORIZE_URL → pixiv://...?code=xxx
     */
    private suspend fun resolvePostRedirect(link: Url): String {
        // GET POST_REDIRECT_URL
        val redirectResp = client.get(link)
        val returnTo = parseHtmlValue(redirectResp.bodyAsText())

        val usersAuthPixivStartUrl = Url(returnTo)
        // POST START_URL (?via=login) → 302 → OAUTH_AUTHORIZE_URL
        val startResp: HttpResponse = client.post(usersAuthPixivStartUrl) {
            header(HttpHeaders.Origin, ORIGIN_URL)
            header(HttpHeaders.Referrer, ORIGIN_URL)
        }
        val authorizeUrl = requireNotNull(startResp.location()) {
            "未能从 START_URL 获取 OAuth Authorize 跳转地址"
        }

        // GET https://oauth.secure.pixiv.net/auth/authorize
        val codeResp: HttpResponse = client.get(authorizeUrl) {
            header(HttpHeaders.Referrer, ORIGIN_URL)
        }
        // https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback?state=xxx&code=xxx
        val usersAuthPixivCallbackUrl = requireNotNull(codeResp.location()) {
            "未能获取 pixiv:// 跳转地址，OAuth 授权失败"
        }

        return usersAuthPixivCallbackUrl.parameters["code"]
            ?: throw NoSuchElementException("pixiv:// URL 中未找到 code 参数，完整 URL: $usersAuthPixivCallbackUrl")
    }

    // ── 工具函数 ───────────────────────────────────────────────────────────────

    /** 解析页面 HTML 中 data-props 和 value 隐藏字段的 JSON */
    private fun parseHtmlAccount(html: String): Pair<String, String> {
        val value = html.substringAfter("value='").substringBefore("'")
        val dataProps = html.substringAfter("data-props=\"")
            .substringBefore("\"")
            .replace("&quot;", "\"")
        val continueUrl = json.parseToJsonElement(dataProps)
            .jsonObject["continueWithCurrentAccountUrl"]
            ?.jsonPrimitive?.content.orEmpty()
        val tt = json.parseToJsonElement(value)
            .jsonObject["pixivAccount.tt"]
            ?.jsonPrimitive?.content.orEmpty()
        return continueUrl to tt
    }

    /** 解析页面 HTML 中 value 隐藏字段的 JSON */
    private fun parseHtmlValue(html: String): String {
        val value = html.substringAfter("value='").substringBefore("'")
        return json.parseToJsonElement(value)
            .jsonObject["pixivAccount.returnTo"]
            ?.jsonPrimitive?.content.orEmpty()
    }

    /** 从响应头中读取 Location */
    private fun HttpResponse.location(): Url? =
        headers[HttpHeaders.Location]?.let(::Url)
}