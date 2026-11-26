package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.Constants.API_HOST
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import org.koin.core.annotation.Single

@Single
@ApiClient
fun apiHttpClient() = routedPixivClient(::createApiClient)

private fun createApiClient(setting: BypassSetting): HttpClient =
    baseHttpClient(setting, allowRedirects = false).applyAppApiRequests(
        readProxyHost = { (currentNetworkPreference() ?: NetworkUtil.networkSettings()).appApiProxyHost },
        readAccessToken = { NetworkUtil.requireUserAccessToken() },
    )

internal fun HttpClient.applyAppApiRequests(
    readProxyHost: suspend () -> String,
    readAccessToken: suspend () -> String,
): HttpClient = apply {
    plugin(HttpSend).intercept { request ->
        require(
            request.url.protocol == URLProtocol.HTTPS &&
                request.url.host.equals(API_HOST, ignoreCase = true) &&
                request.url.user == null && request.url.password == null,
        ) { "Unexpected Pixiv App API request destination" }
        request.url.applyAppApiProxyHost(readProxyHost())
        NetworkUtil.addAuthHeader(request, readAccessToken)
        var call = execute(request)
        if (call.response.status.shouldRetryAppApiRequest()) {
            // 重试在途请求时保留原账号的认证凭据，仅刷新签名请求头，避免对 429/403/404 等状态码零退避重试。
            NetworkUtil.addAppHeaders(request)
            call = execute(request)
        }
        call
    }
}

internal fun HttpStatusCode.shouldRetryAppApiRequest(): Boolean =
    this == HttpStatusCode.BadRequest || this == HttpStatusCode.Unauthorized
