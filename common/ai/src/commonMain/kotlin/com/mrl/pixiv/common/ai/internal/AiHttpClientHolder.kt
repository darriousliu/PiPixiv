package com.mrl.pixiv.common.ai.internal

import com.mrl.pixiv.common.network.httpEngineFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import org.koin.core.annotation.Single

@Single
class AiHttpClientHolder {
    val client = HttpClient(httpEngineFactory) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }
    }
}
