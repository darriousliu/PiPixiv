package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.Constants.AUTH_HOST
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.serialize.JSON
import com.mrl.pixiv.common.util.isDebug
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import co.touchlab.kermit.Logger as KermitLogger

internal fun <T : HttpClientEngineConfig> httpClient(
    engine: HttpClientEngineFactory<T>,
    config: T.() -> Unit
): HttpClient {
    return HttpClient(engine) {
        followRedirects = false

        engine { config() }

        // Keep API responses out of Ktor's default unbounded in-memory HTTP cache.

        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }

        install(ContentNegotiation) {
            json(JSON)
        }

        install(Logging) {
            level = if (isDebug) LogLevel.ALL else LogLevel.NONE
            filter { request ->
                request.url.host != AUTH_HOST || request.url.encodedPath.trimEnd('/') != "/auth/token"
            }
            sanitizeHeader { it.equals(HttpHeaders.Authorization, ignoreCase = true) }
            logger =
                object : Logger {
                    override fun log(message: String) {
                        KermitLogger.d(tag = "HttpClient") {
                            """
              |---
              |$message
              |---
            """
                                .trimMargin("|")
                        }
                    }
                }
        }
    }
}

internal fun <T : HttpClientEngineConfig> imageHttpClient(
    engine: HttpClientEngineFactory<T>,
    config: T.() -> Unit
) = HttpClient(engine) {
    followRedirects = false

    defaultRequest {
        header("Referer", "https://www.pixiv.net/")
    }

    engine { config() }

    install(HttpTimeout) {
        requestTimeoutMillis = 30000
        connectTimeoutMillis = 30000
        socketTimeoutMillis = 30000
    }
}

internal expect fun baseHttpClient(
    setting: UserPreference.BypassSetting,
    allowRedirects: Boolean = true,
): HttpClient

internal expect fun baseImageHttpClient(setting: UserPreference.BypassSetting): HttpClient

expect val httpEngineFactory: HttpClientEngineFactory<*>

// 非 Pixiv 客户端共享代理选择，但不启用 Pixiv 专用的 ECH 处理。
expect fun HttpClientConfig<*>.configureNetworkProxy(
    setting: UserPreference.BypassSetting = NetworkUtil.bypassSetting,
)

expect fun HttpClientConfig<*>.configurePixivNetworkProxy(setting: UserPreference.BypassSetting)

internal fun routedPixivClient(create: (UserPreference.BypassSetting) -> HttpClient) =
    RoutedHttpClient(NetworkUtil::bypassSetting, create, HttpClient::close, NetworkUtil::networkSettings)
