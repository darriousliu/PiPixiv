package com.mrl.pixiv.common.ai.internal

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AiHttpClientConfigTest {
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
