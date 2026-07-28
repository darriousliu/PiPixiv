package com.mrl.pixiv.common.ai.internal

import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import com.mrl.pixiv.common.network.httpEngineFactory
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import org.koin.core.annotation.Single

internal const val AI_GENERATION_TIMEOUT_MILLIS =
    AiTranslationConfig.GENERATION_TIMEOUT_DEFAULT_SECONDS * 1_000L
internal const val AI_CONNECT_TIMEOUT_MILLIS = 60_000L

@Single
class AiHttpClientHolder {
    val client = HttpClient(httpEngineFactory) {
        configureAiHttpClient()
    }
}

internal fun HttpClientConfig<*>.configureAiHttpClient() {
    followRedirects = false
    install(HttpTimeout) {
        requestTimeoutMillis = AI_GENERATION_TIMEOUT_MILLIS
        connectTimeoutMillis = AI_CONNECT_TIMEOUT_MILLIS
        socketTimeoutMillis = AI_GENERATION_TIMEOUT_MILLIS
    }
    install(SSE)
}

internal fun HttpRequestBuilder.configureAiGenerationTimeout(
    generationTimeoutMillis: Long,
    streaming: Boolean = false,
) {
    timeout {
        requestTimeoutMillis = if (streaming) {
            HttpTimeoutConfig.INFINITE_TIMEOUT_MS
        } else {
            generationTimeoutMillis
        }
        connectTimeoutMillis = AI_CONNECT_TIMEOUT_MILLIS
        socketTimeoutMillis = generationTimeoutMillis
    }
}
