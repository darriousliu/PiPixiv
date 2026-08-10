package com.mrl.pixiv.common.ai

import com.mrl.pixiv.common.ai.internal.AiHttpClientHolder
import com.mrl.pixiv.common.data.setting.AiProvider
import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AiModelCatalogServiceTest {
    private val service = AiModelCatalogService(AiHttpClientHolder())

    @Test
    fun openAiUsesModelsEndpointAndBearerAuthentication() = runTest {
        val client = mockClient { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/v1/models", request.url.encodedPath)
            assertEquals("Bearer openai-key", request.headers[HttpHeaders.Authorization])
            respondJson(
                """
                    {
                      "data": [
                        {"id": "gpt-5.4-mini"},
                        {"id": "gpt-5.4"},
                        {"id": "gpt-5.4-mini"}
                      ]
                    }
                """.trimIndent()
            )
        }

        try {
            assertEquals(
                listOf("gpt-5.4-mini", "gpt-5.4"),
                service.fetchModels(
                    config = config(
                        provider = AiProvider.OPENAI,
                        endpoint = "https://api.openai.com/v1/",
                        apiKey = " openai-key ",
                    ),
                    httpClient = client,
                ),
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun claudeUsesVersionedEndpointAndRequiredHeaders() = runTest {
        val client = mockClient { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/v1/models", request.url.encodedPath)
            assertEquals("claude-key", request.headers["x-api-key"])
            assertEquals("2023-06-01", request.headers["anthropic-version"])
            respondJson(
                """
                    {
                      "data": [
                        {"type": "model", "id": "claude-opus-4-8"},
                        {"type": "model", "id": "claude-sonnet-4-6"}
                      ],
                      "has_more": false
                    }
                """.trimIndent()
            )
        }

        try {
            assertEquals(
                listOf("claude-opus-4-8", "claude-sonnet-4-6"),
                service.fetchModels(
                    config = config(
                        provider = AiProvider.CLAUDE,
                        endpoint = "https://api.anthropic.com",
                        apiKey = "claude-key",
                    ),
                    httpClient = client,
                ),
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun geminiUsesV1BetaEndpointAndApiKeyHeader() = runTest {
        val client = mockClient { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/v1beta/models", request.url.encodedPath)
            assertEquals("gemini-key", request.headers["x-goog-api-key"])
            respondJson(
                """
                    {
                      "models": [
                        {"name": "models/gemini-3.5-flash"},
                        {"name": "models/gemini-3.1-pro-preview"}
                      ]
                    }
                """.trimIndent()
            )
        }

        try {
            assertEquals(
                listOf("gemini-3.5-flash", "gemini-3.1-pro-preview"),
                service.fetchModels(
                    config = config(
                        provider = AiProvider.GEMINI,
                        endpoint = "https://generativelanguage.googleapis.com",
                        apiKey = "gemini-key",
                    ),
                    httpClient = client,
                ),
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun httpErrorIsPropagated() = runTest {
        val client = mockClient {
            respondJson(
                content = """{"error":{"message":"invalid key"}}""",
                status = HttpStatusCode.Unauthorized,
            )
        }

        try {
            val error = assertFailsWith<AiHttpStatusException> {
                service.fetchModels(
                    config = config(AiProvider.OPENAI, "https://example.com", "bad-key"),
                    httpClient = client,
                )
            }
            assertEquals(HttpStatusCode.Unauthorized.value, error.statusCode)
        } finally {
            client.close()
        }
    }

    @Test
    fun malformedProviderShapeIsRejected() = runTest {
        val client = mockClient {
            respondJson("""{"models":"not-an-array"}""")
        }

        try {
            assertFailsWith<IllegalStateException> {
                service.fetchModels(
                    config = config(AiProvider.GEMINI, "https://example.com", "key"),
                    httpClient = client,
                )
            }
        } finally {
            client.close()
        }
    }

    private fun config(
        provider: AiProvider,
        endpoint: String,
        apiKey: String,
    ): AiTranslationConfig {
        return AiTranslationConfig(
            provider = provider,
            endpoint = endpoint,
            apiKey = apiKey,
        )
    }

    private fun mockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HttpClient {
        return HttpClient(MockEngine(handler))
    }

    private fun MockRequestHandleScope.respondJson(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpResponseData {
        return respond(
            content = content,
            status = status,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }
}
