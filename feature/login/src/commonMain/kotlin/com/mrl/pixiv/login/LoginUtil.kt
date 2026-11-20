package com.mrl.pixiv.login

import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.analytics.recordCrashOperation
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import okio.ByteString.Companion.toByteString
import kotlin.io.encoding.Base64
import kotlin.random.Random

private var codeVerifier = getCodeVer()

private fun getCodeVer(): String {
    val randomKeySet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    val result = (0..128).map {
        randomKeySet[Random.nextInt(randomKeySet.length)]
    }.joinToString("")
    codeVerifier = result
    return codeVerifier
}

private fun getCodeChallenge(): String {
    return Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(
        getCodeVer().encodeToByteArray().toByteString().sha256().toByteArray(),
    )
}

fun generateWebViewUrl(create: Boolean) =
    if (create) {
        "https://app-api.pixiv.net/web/v1/provisional-accounts/create?code_challenge=${getCodeChallenge()}&code_challenge_method=S256&client=pixiv-android"
    } else {
        "https://app-api.pixiv.net/web/v1/login?code_challenge=${getCodeChallenge()}&code_challenge_method=S256&client=pixiv-android"
    }

fun checkUri(url: String): Pair<String, String>? {
    val url = try {
        Url(url)
    } catch (_: Exception) {
        return null
    }
    if (url.protocol.name == "pixiv" && url.host == "account") {
        val code = url.parameters["code"]
        return if (code != null) code to codeVerifier else null
    }
    return null
}

internal suspend fun recordLoginAttempt(login: suspend () -> Unit): Boolean {
    recordCrashOperation("LoginStarted")
    try {
        login()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Logger.e(tag = "Login") { "Login failed: ${error::class.simpleName ?: "Exception"}" }
        recordCrashOperation("LoginFailed")
        return false
    }
    recordCrashOperation("LoginSucceeded")
    return true
}
