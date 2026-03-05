package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.serialize.JSON
import com.mrl.pixiv.common.util.isDebug
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import co.touchlab.kermit.Logger as KermitLogger

internal fun <T : HttpClientEngineConfig> httpClient(
    engine: HttpClientEngineFactory<T>,
    config: T.() -> Unit
): HttpClient {
    return HttpClient(engine) {
        followRedirects = false

        engine { config() }

        install(HttpCache)

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
            logger =
                object : Logger {
                    override fun log(message: String) {
                        KermitLogger.i("HttpClient") {
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

internal expect val baseHttpClient: HttpClient

internal expect val baseImageHttpClient: HttpClient

expect val httpEngineFactory: HttpClientEngineFactory<*>