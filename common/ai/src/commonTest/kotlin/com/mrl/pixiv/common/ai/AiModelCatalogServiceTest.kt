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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
                        {
                          "name": "models/gemini-3.5-flash",
                          "supportedGenerationMethods": ["generateContent"]
                        },
                        {
                          "name": "models/gemini-3.1-pro-preview",
                          "supportedGenerationMethods": ["generateContent"]
                        }
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
    fun claudeFetchesEveryPageWithAuthenticationAndDeduplicatesModels() = runTest {
        val requestedCursors = mutableListOf<String?>()
        val client = mockClient { request ->
            assertEquals("/v1/models", request.url.encodedPath)
            assertEquals("claude-key", request.headers["x-api-key"])
            assertEquals("2023-06-01", request.headers["anthropic-version"])
            requestedCursors += request.url.parameters["after_id"]
            respondJson(
                when (requestedCursors.size) {
                    1 -> """
                        {
                          "data": [{"id":"claude-opus-4-8"}],
                          "has_more": true,
                          "last_id": "claude-opus-4-8"
                        }
                    """.trimIndent()

                    2 -> """
                        {
                          "data": [{"id":"claude-opus-4-8"},{"id":"claude-sonnet-4-6"}],
                          "has_more": false,
                          "last_id": "claude-sonnet-4-6"
                        }
                    """.trimIndent()

                    else -> error("Requested a page after has_more was false")
                }
            )
        }

        try {
            assertEquals(
                listOf("claude-opus-4-8", "claude-sonnet-4-6"),
                service.fetchModels(
                    config(AiProvider.CLAUDE, "https://api.anthropic.com", "claude-key"),
                    client,
                ),
            )
            assertEquals(listOf(null, "claude-opus-4-8"), requestedCursors)
        } finally {
            client.close()
        }
    }

    @Test
    fun geminiPreservesOpaquePageTokensAndAuthenticationAcrossPages() = runTest {
        val requestedTokens = mutableListOf<String?>()
        val client = mockClient { request ->
            assertEquals("/v1beta/models", request.url.encodedPath)
            assertEquals("gemini-key", request.headers["x-goog-api-key"])
            requestedTokens += request.url.parameters["pageToken"]
            respondJson(
                when (requestedTokens.size) {
                    1 -> """
                        {
                          "models": [{
                            "name":"models/gemini-3.5-flash",
                            "supportedGenerationMethods":["generateContent"]
                          }],
                          "nextPageToken": "next+/=&page"
                        }
                    """.trimIndent()

                    2 -> """
                        {
                          "models": [
                            {
                              "name":"models/gemini-3.5-flash",
                              "supportedGenerationMethods":["generateContent"]
                            },
                            {
                              "name":"models/gemini-3.1-pro-preview",
                              "supportedGenerationMethods":["generateContent"]
                            }
                          ],
                          "nextPageToken": ""
                        }
                    """.trimIndent()

                    else -> error("Requested a page after the token was empty")
                }
            )
        }

        try {
            assertEquals(
                listOf("gemini-3.5-flash", "gemini-3.1-pro-preview"),
                service.fetchModels(
                    config(AiProvider.GEMINI, "https://example.com", "gemini-key"),
                    client,
                ),
            )
            assertEquals(listOf(null, "next+/=&page"), requestedTokens)
        } finally {
            client.close()
        }
    }

    @Test
    fun geminiListsOnlyModelsThatAdvertiseGenerateContent() = runTest {
        val client = mockClient {
            respondJson(
                """
                    {
                      "models": [
                        {
                          "name":"models/gemini-embedding-001",
                          "supportedGenerationMethods":["embedContent","countTokens"]
                        },
                        {
                          "name":"models/gemini-3.5-flash",
                          "supportedGenerationMethods":["countTokens","generateContent"]
                        },
                        {
                          "name":"models/image-generation-model",
                          "supportedGenerationMethods":["predict"]
                        },
                        {
                          "name":"models/gemini-3.1-pro-preview",
                          "supportedGenerationMethods":["generateContent"]
                        }
                      ]
                    }
                """.trimIndent()
            )
        }

        try {
            assertEquals(
                listOf("gemini-3.5-flash", "gemini-3.1-pro-preview"),
                service.fetchModels(
                    config(AiProvider.GEMINI, "https://example.com", "key"),
                    client,
                ),
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun geminiFetchesNextPageEvenWhenEveryModelOnFirstPageIsFilteredOut() = runTest {
        val requestedTokens = mutableListOf<String?>()
        val client = mockClient { request ->
            requestedTokens += request.url.parameters["pageToken"]
            respondJson(
                when (requestedTokens.size) {
                    1 -> """
                        {
                          "models":[{
                            "name":"models/gemini-embedding-001",
                            "supportedGenerationMethods":["embedContent"]
                          }],
                          "nextPageToken":"text-models-page"
                        }
                    """.trimIndent()

                    2 -> """
                        {
                          "models":[{
                            "name":"models/gemini-3.5-flash",
                            "supportedGenerationMethods":["generateContent"]
                          }]
                        }
                    """.trimIndent()

                    else -> error("Requested a page after the final page")
                }
            )
        }

        try {
            assertEquals(
                listOf("gemini-3.5-flash"),
                service.fetchModels(
                    config(AiProvider.GEMINI, "https://example.com", "key"),
                    client,
                ),
            )
            assertEquals(listOf(null, "text-models-page"), requestedTokens)
        } finally {
            client.close()
        }
    }

    @Test
    fun geminiReturnsEmptyWhenNoModelsHaveKnownTextGenerationSupport() = runTest {
        var requestCount = 0
        val client = mockClient {
            requestCount += 1
            respondJson(
                """
                    {
                      "models":[
                        {
                          "name":"models/gemini-embedding-001",
                          "supportedGenerationMethods":["embedContent"]
                        },
                        {"name":"models/compatibility-model-without-capabilities"},
                        {"name":"models/empty-capabilities","supportedGenerationMethods":[]},
                        {"name":"models/null-capabilities","supportedGenerationMethods":null},
                        {
                          "name":"models/malformed-capabilities",
                          "supportedGenerationMethods":"generateContent"
                        }
                      ]
                    }
                """.trimIndent()
            )
        }

        try {
            assertEquals(
                emptyList(),
                service.fetchModels(
                    config(AiProvider.GEMINI, "https://example.com", "key"),
                    client,
                ),
            )
            assertEquals(1, requestCount)
        } finally {
            client.close()
        }
    }

    @Test
    fun cursorCyclesFailInsteadOfRepeatingRequestsOrReturningPartialResults() = runTest {
        for (provider in listOf(AiProvider.CLAUDE, AiProvider.GEMINI)) {
            var requestCount = 0
            val client = mockClient {
                val cursor = when (++requestCount) {
                    1, 3 -> "first-cursor"
                    2 -> "second-cursor"
                    else -> error("Repeated a previously requested cursor")
                }
                respondJson(pageWithNextCursor(provider, cursor))
            }

            try {
                val error = assertFailsWith<IllegalStateException> {
                    service.fetchModels(config(provider, "https://example.com", "key"), client)
                }
                assertTrue(error.message.orEmpty().contains("repeated"))
                assertEquals(3, requestCount)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun claudeRejectsMissingCursorWhenMorePagesAreAdvertised() = runTest {
        var requestCount = 0
        val client = mockClient {
            requestCount += 1
            respondJson("""{"data":[{"id":"claude-sonnet-4-6"}],"has_more":true}""")
        }

        try {
            assertFailsWith<IllegalStateException> {
                service.fetchModels(
                    config(AiProvider.CLAUDE, "https://example.com", "key"),
                    client,
                )
            }
            assertEquals(1, requestCount)
        } finally {
            client.close()
        }
    }

    @Test
    fun laterPageHttpFailureIsPropagatedWithoutReturningPartialModels() = runTest {
        for (provider in listOf(AiProvider.CLAUDE, AiProvider.GEMINI)) {
            var requestCount = 0
            val client = mockClient {
                if (++requestCount == 1) {
                    respondJson(pageWithNextCursor(provider, "next-cursor"))
                } else {
                    respondJson(
                        content = """{"error":{"message":"rate limit reached"}}""",
                        status = HttpStatusCode.TooManyRequests,
                    )
                }
            }

            try {
                val error = assertFailsWith<AiHttpStatusException> {
                    service.fetchModels(config(provider, "https://example.com", "key"), client)
                }
                assertEquals(HttpStatusCode.TooManyRequests.value, error.statusCode)
                assertEquals(2, requestCount)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun cancellationStopsFetchingLaterPages() = runTest {
        for (provider in listOf(AiProvider.CLAUDE, AiProvider.GEMINI)) {
            var requestCount = 0
            val secondPageStarted = CompletableDeferred<Unit>()
            val client = mockClient {
                if (++requestCount == 1) {
                    respondJson(pageWithNextCursor(provider, "next-cursor"))
                } else {
                    secondPageStarted.complete(Unit)
                    awaitCancellation()
                }
            }

            try {
                val request = async {
                    service.fetchModels(config(provider, "https://example.com", "key"), client)
                }
                secondPageStarted.await()
                request.cancelAndJoin()

                assertFailsWith<CancellationException> { request.await() }
                assertEquals(2, requestCount)
            } finally {
                client.close()
            }
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

    private fun pageWithNextCursor(provider: AiProvider, cursor: String): String = when (provider) {
        AiProvider.CLAUDE ->
            """{"data":[{"id":"claude-sonnet-4-6"}],"has_more":true,"last_id":"$cursor"}"""

        AiProvider.GEMINI ->
            """
                {
                  "models":[{
                    "name":"models/gemini-3.5-flash",
                    "supportedGenerationMethods":["generateContent"]
                  }],
                  "nextPageToken":"$cursor"
                }
            """.trimIndent()

        AiProvider.OPENAI -> error("OpenAI model catalog does not use cursor pagination")
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
