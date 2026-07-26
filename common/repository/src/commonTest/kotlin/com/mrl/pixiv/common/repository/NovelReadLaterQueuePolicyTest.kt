package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.ai.AiLocalNetworkAccessState
import com.mrl.pixiv.common.ai.AiHttpStatusException
import com.mrl.pixiv.common.data.setting.AiProvider
import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import com.mrl.pixiv.common.data.setting.BrowsingSettings
import com.mrl.pixiv.common.datasource.local.entity.NovelReadLaterEntity
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.sse.SSEClientException
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NovelReadLaterQueuePolicyTest {
    @Test
    fun `local endpoint is not claimed before runtime permission is granted`() {
        val localEndpoint = "http://192.168.1.20:11434"

        assertFalse(
            NovelReadLaterQueuePolicy.canClaimEndpoint(
                endpoint = localEndpoint,
                localNetworkAccessState = AiLocalNetworkAccessState.UNDETERMINED,
            )
        )
        assertFalse(
            NovelReadLaterQueuePolicy.canClaimEndpoint(
                endpoint = localEndpoint,
                localNetworkAccessState = AiLocalNetworkAccessState.DENIED,
            )
        )
        assertTrue(
            NovelReadLaterQueuePolicy.canClaimEndpoint(
                endpoint = localEndpoint,
                localNetworkAccessState = AiLocalNetworkAccessState.GRANTED,
            )
        )
        assertTrue(
            NovelReadLaterQueuePolicy.canClaimEndpoint(
                endpoint = "https://api.openai.com",
                localNetworkAccessState = AiLocalNetworkAccessState.DENIED,
            )
        )
        assertFalse(
            NovelReadLaterQueuePolicy.canClaimEndpoint(
                endpoint = "not a URL",
                localNetworkAccessState = AiLocalNetworkAccessState.GRANTED,
            )
        )
    }

    @Test
    fun `blocked local task does not prevent later https task from being claimed`() {
        val endpoints = listOf(
            "http://192.168.1.20:11434",
            "https://api.openai.com",
        )

        assertEquals(
            1,
            NovelReadLaterQueuePolicy.firstClaimableEndpointIndex(
                endpoints = endpoints,
                localNetworkAccessState = AiLocalNetworkAccessState.DENIED,
            )
        )
        assertTrue(
            NovelReadLaterQueuePolicy.needsLocalNetworkAccess(
                endpoints = endpoints,
                localNetworkAccessState = AiLocalNetworkAccessState.DENIED,
            )
        )
        assertEquals(
            0,
            NovelReadLaterQueuePolicy.firstClaimableEndpointIndex(
                endpoints = endpoints,
                localNetworkAccessState = AiLocalNetworkAccessState.GRANTED,
            )
        )
    }

    @Test
    fun `transient failures retry three times then fail`() {
        val first = NovelReadLaterQueuePolicy.failureTransition(
            retryCount = 0,
            isTransient = true,
        )
        val second = NovelReadLaterQueuePolicy.failureTransition(
            retryCount = first.retryCount,
            isTransient = true,
        )
        val third = NovelReadLaterQueuePolicy.failureTransition(
            retryCount = second.retryCount,
            isTransient = true,
        )
        val fourth = NovelReadLaterQueuePolicy.failureTransition(
            retryCount = third.retryCount,
            isTransient = true,
        )

        assertEquals(NovelReadLaterState.PENDING, first.state)
        assertEquals(NovelReadLaterState.PENDING, second.state)
        assertEquals(NovelReadLaterState.PENDING, third.state)
        assertEquals(3, third.retryCount)
        assertEquals(NovelReadLaterState.FAILED, fourth.state)
        assertEquals(3, fourth.retryCount)
    }

    @Test
    fun `permanent failure does not retry`() {
        val transition = NovelReadLaterQueuePolicy.failureTransition(
            retryCount = 0,
            isTransient = false,
        )
        assertEquals(NovelReadLaterState.FAILED, transition.state)
        assertEquals(0, transition.retryCount)
    }

    @Test
    fun `ready cache requires exact config and source fingerprints`() {
        assertTrue(
            NovelReadLaterQueuePolicy.isExactReadyCache(
                state = NovelReadLaterState.READY,
                taskConfigFingerprint = "config",
                currentConfigFingerprint = "config",
                cacheConfigFingerprint = "config",
                taskSourceMd5 = "source",
                currentSourceMd5 = "source",
                cacheSourceMd5 = "source",
            )
        )
        assertFalse(
            NovelReadLaterQueuePolicy.isExactReadyCache(
                state = NovelReadLaterState.PENDING,
                taskConfigFingerprint = "config",
                currentConfigFingerprint = "config",
                cacheConfigFingerprint = "config",
                taskSourceMd5 = "source",
                currentSourceMd5 = "source",
                cacheSourceMd5 = "source",
            )
        )
        assertFalse(
            NovelReadLaterQueuePolicy.isExactReadyCache(
                state = NovelReadLaterState.READY,
                taskConfigFingerprint = "old",
                currentConfigFingerprint = "new",
                cacheConfigFingerprint = "new",
                taskSourceMd5 = "source",
                currentSourceMd5 = "source",
                cacheSourceMd5 = "source",
            )
        )
        assertFalse(
            NovelReadLaterQueuePolicy.isExactReadyCache(
                state = NovelReadLaterState.READY,
                taskConfigFingerprint = "config",
                currentConfigFingerprint = "config",
                cacheConfigFingerprint = "config",
                taskSourceMd5 = "old",
                currentSourceMd5 = "new",
                cacheSourceMd5 = "new",
            )
        )
        assertFalse(
            NovelReadLaterQueuePolicy.isExactReadyCache(
                state = NovelReadLaterState.READY,
                taskConfigFingerprint = "config",
                currentConfigFingerprint = "config",
                cacheConfigFingerprint = "other-config",
                taskSourceMd5 = "source",
                currentSourceMd5 = "source",
                cacheSourceMd5 = "source",
            )
        )
        assertFalse(
            NovelReadLaterQueuePolicy.isExactReadyCache(
                state = NovelReadLaterState.READY,
                taskConfigFingerprint = "config",
                currentConfigFingerprint = "config",
                cacheConfigFingerprint = "config",
                taskSourceMd5 = "source",
                currentSourceMd5 = "source",
                cacheSourceMd5 = "other-source",
            )
        )
    }

    @Test
    fun `worker classifies retryable AI http statuses`() {
        listOf(408, 429, 500, 502, 599).forEach { status ->
            assertTrue(
                AiHttpStatusException(
                    statusCode = status,
                    message = "request failed",
                ).isTransientQueueFailure(),
                "Expected HTTP $status to be transient",
            )
        }
        listOf(400, 401, 403, 404, 422, 600).forEach { status ->
            assertFalse(
                AiHttpStatusException(
                    statusCode = status,
                    message = "request failed",
                ).isTransientQueueFailure(),
                "Expected HTTP $status to be permanent",
            )
        }

        val retryableTransition = NovelReadLaterQueuePolicy.failureTransition(
            retryCount = 0,
            isTransient = AiHttpStatusException(429, "rate limited")
                .isTransientQueueFailure(),
        )
        assertEquals(NovelReadLaterState.PENDING, retryableTransition.state)
        assertEquals(1, retryableTransition.retryCount)
    }

    @Test
    fun `worker classifies real SSE status and IO failures`() = runTest {
        listOf(
            HttpStatusCode.RequestTimeout,
            HttpStatusCode.TooManyRequests,
            HttpStatusCode.InternalServerError,
            HttpStatusCode(599, "Network Connect Timeout Error"),
        ).forEach { status ->
            val failure = createSseStatusFailure(status)
            assertTrue(
                failure.isTransientQueueFailure(),
                "Expected SSE HTTP ${status.value} to be transient; " +
                        "response=${failure.response?.status?.value}, " +
                        "cause=${failure.cause}, message=${failure.message}",
            )
        }
        assertFalse(
            createSseStatusFailure(HttpStatusCode.BadRequest).isTransientQueueFailure()
        )
        assertTrue(
            SSEClientException(
                cause = IOException("socket closed"),
                message = "SSE transport failed",
            ).isTransientQueueFailure()
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `read later items react to long tag setting changes`() = runTest {
        val entries = MutableStateFlow(
            listOf(
                queueEntity(novelId = 1L, tags = listOf("short")),
                queueEntity(novelId = 2L, tags = listOf("x".repeat(31))),
            )
        )
        val settings = MutableStateFlow(BrowsingSettings())
        val emissions = mutableListOf<List<NovelReadLaterEntity>>()
        val collection = launch {
            filterNovelReadLaterEntries(entries, settings)
                .take(2)
                .toList(emissions)
        }
        advanceUntilIdle()

        settings.value = BrowsingSettings(filterLongNovelTags = true)
        advanceUntilIdle()
        collection.join()

        assertEquals(listOf(1L, 2L), emissions[0].map { it.novelId })
        assertEquals(listOf(1L), emissions[1].map { it.novelId })
    }

    @Test
    fun `read later tag snapshot round trips without secrets`() {
        val tags = listOf("plain", "slash/tag", "日本語")
        val encoded = encodeNovelReadLaterTags(tags)

        assertEquals(tags, decodeNovelReadLaterTags(encoded))
        assertEquals(emptyList(), decodeNovelReadLaterTags("not JSON"))
        assertFalse(encoded.contains("api-key"))
    }

    @Test
    fun `config fingerprint covers secret without storing it as plaintext`() {
        val first = config(apiKey = "secret-one")
        val second = config(apiKey = "secret-two")
        val firstFingerprint = buildNovelAiConfigFingerprint(first)

        assertNotEquals(
            firstFingerprint,
            buildNovelAiConfigFingerprint(second),
        )
        assertFalse(firstFingerprint.contains(first.apiKey))
        assertEquals(64, firstFingerprint.length)
    }

    @Test
    fun `source hash includes normalized extra body`() {
        val withoutExtra = buildNovelTranslationSourceHash("source", "")
        val withExtra = buildNovelTranslationSourceHash(
            sourceText = "source",
            extraBody = """{"temperature":0.2}""",
        )
        assertNotEquals(withoutExtra, withExtra)
        assertEquals(
            withExtra,
            buildNovelTranslationSourceHash(
                sourceText = "source",
                extraBody = """  {"temperature":0.2}  """,
            )
        )
    }

    private fun config(apiKey: String) = AiTranslationConfig(
        provider = AiProvider.OPENAI,
        endpoint = "https://example.com/v1/",
        apiKey = apiKey,
        model = "model",
        responseApi = true,
        extraBody = """{"temperature":0.2}""",
    )

    private suspend fun createSseStatusFailure(
        status: HttpStatusCode,
    ): SSEClientException {
        val client = HttpClient(
            MockEngine {
                respond(
                    content = "",
                    status = status,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        "text/event-stream",
                    ),
                )
            }
        )
        return try {
            SSEClientException(
                response = client.get("https://api.example.com/stream"),
                message = "Expected status code 200 but was ${status.value}",
            )
        } finally {
            client.close()
        }
    }

    private fun queueEntity(
        novelId: Long,
        tags: List<String>,
    ) = NovelReadLaterEntity(
        novelId = novelId,
        userId = 1L,
        targetLanguage = "en",
        novelTitle = "Novel $novelId",
        novelCaption = "Summary",
        novelAuthorName = "Author",
        coverUrl = "https://example.com/cover.jpg",
        novelTagsJson = encodeNovelReadLaterTags(tags),
        addedAtMillis = novelId,
        provider = "OPENAI",
        model = "model",
        endpoint = "https://example.com/v1",
        responseApi = false,
        extraBody = "",
        configFingerprint = "fingerprint",
        sourceMd5 = "",
        state = "PENDING",
        attemptToken = "",
        retryCount = 0,
        lastError = null,
        updatedAtMillis = 1L,
    )
}
