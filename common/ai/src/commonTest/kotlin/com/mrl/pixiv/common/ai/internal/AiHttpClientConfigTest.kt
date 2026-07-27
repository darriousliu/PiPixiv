package com.mrl.pixiv.common.ai.internal

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AiHttpClientConfigTest {
    @Test
    fun ordinaryGenerationUsesPerRequestTimeouts() {
        val request = HttpRequestBuilder().apply {
            configureAiGenerationTimeout(generationTimeoutMillis = 900_000L)
        }

        val timeout = assertNotNull(request.getCapabilityOrNull(HttpTimeoutCapability))
        assertEquals(900_000L, timeout.requestTimeoutMillis)
        assertEquals(AI_CONNECT_TIMEOUT_MILLIS, timeout.connectTimeoutMillis)
        assertEquals(900_000L, timeout.socketTimeoutMillis)
    }

    @Test
    fun streamingGenerationHasInfiniteTotalAndPerRequestIdleTimeout() {
        val request = HttpRequestBuilder().apply {
            configureAiGenerationTimeout(
                generationTimeoutMillis = 1_800_000L,
                streaming = true,
            )
        }

        val timeout = assertNotNull(request.getCapabilityOrNull(HttpTimeoutCapability))
        assertEquals(HttpTimeoutConfig.INFINITE_TIMEOUT_MS, timeout.requestTimeoutMillis)
        assertEquals(AI_CONNECT_TIMEOUT_MILLIS, timeout.connectTimeoutMillis)
        assertEquals(1_800_000L, timeout.socketTimeoutMillis)
    }

    @Test
    fun redirectToPublicHttpIsNotFollowed() = runTest {
        val visitedUrls = mutableListOf<String>()
        val engine = MockEngine { request ->
            visitedUrls += request.url.toString()
            if (visitedUrls.size == 1) {
                respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf(
                        HttpHeaders.Location,
                        "http://8.8.8.8/v1/chat/completions",
                    ),
                )
            } else {
                respond(content = "redirect followed")
            }
        }
        val client = HttpClient(engine) {
            configureAiHttpClient()
        }

        try {
            val response = client.get("http://127.0.0.1/start")

            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals(
                listOf("http://127.0.0.1/start"),
                visitedUrls,
            )
        } finally {
            client.close()
        }
    }
}
