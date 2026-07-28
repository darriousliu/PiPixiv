package com.mrl.pixiv.common.ai.provider

import com.mrl.pixiv.common.ai.AiMessageRole
import com.mrl.pixiv.common.ai.AiTextMessage
import com.mrl.pixiv.common.ai.AiTextRequest
import com.mrl.pixiv.common.ai.internal.AiHttpClientHolder
import com.mrl.pixiv.common.ai.model.OpenAiApiType
import com.mrl.pixiv.common.data.setting.AiProvider
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenAiStreamParserTest {
    @Test
    fun terminalFrameStopsCollectionEvenWhenTransportStaysOpen() = runTest {
        val events = openAiStreamEvents(
            dataFrames = flow {
                emit("""{"choices":[{"delta":{"content":"done"},"finish_reason":null}]}""")
                emit("[DONE]")
                awaitCancellation()
            },
            apiType = OpenAiApiType.CHAT_COMPLETIONS,
        ).toList()

        assertEquals(
            listOf(
                AiTextStreamEvent.Delta("done"),
                AiTextStreamEvent.Completed("done"),
            ),
            events,
        )
    }

    @Test
    fun parsesChatCompletionsDeltaAndFinish() {
        val delta = parseOpenAiStreamFrame(
            data = """{"choices":[{"delta":{"content":"你好"},"finish_reason":null}]}""",
            apiType = OpenAiApiType.CHAT_COMPLETIONS,
        )
        val finished = parseOpenAiStreamFrame(
            data = """{"choices":[{"delta":{},"finish_reason":"stop"}]}""",
            apiType = OpenAiApiType.CHAT_COMPLETIONS,
        )

        assertEquals("你好", delta.delta)
        assertTrue(!delta.completed)
        assertTrue(finished.completed)
    }

    @Test
    fun parsesResponsesDeltaAndCompletion() {
        val delta = parseOpenAiStreamFrame(
            data = """{"type":"response.output_text.delta","delta":"Hello"}""",
            apiType = OpenAiApiType.RESPONSES,
        )
        val completed = parseOpenAiStreamFrame(
            data = """{"type":"response.completed","response":{"id":"resp_1"}}""",
            apiType = OpenAiApiType.RESPONSES,
        )

        assertEquals("Hello", delta.delta)
        assertTrue(completed.completed)
    }

    @Test
    fun doneMarkerCompletesEitherApi() {
        assertTrue(
            parseOpenAiStreamFrame("[DONE]", OpenAiApiType.CHAT_COMPLETIONS).completed
        )
        assertTrue(
            parseOpenAiStreamFrame(" [DONE] ", OpenAiApiType.RESPONSES).completed
        )
    }

    @Test
    fun propagatesProviderAndMalformedEventErrors() {
        val providerError = parseOpenAiStreamFrame(
            data = """{"error":{"message":"rate limited"}}""",
            apiType = OpenAiApiType.CHAT_COMPLETIONS,
        )
        val malformed = parseOpenAiStreamFrame(
            data = "not-json",
            apiType = OpenAiApiType.RESPONSES,
        )

        assertEquals("rate limited", providerError.error)
        assertNotNull(malformed.error)
    }

    @Test
    fun extraBodyCannotDisableStreaming() {
        val client = OpenAiTextClient(AiHttpClientHolder())
        val body = client.buildStreamingBody(
            request = AiTextRequest(
                provider = AiProvider.OPENAI,
                endpoint = "https://example.com",
                apiKey = "key",
                model = "model",
                messages = listOf(AiTextMessage(AiMessageRole.USER, "hello")),
                extraBody = """{"stream":false,"temperature":0.25}""",
            ),
            apiType = OpenAiApiType.CHAT_COMPLETIONS,
        )

        assertTrue(body.getValue("stream").jsonPrimitive.boolean)
        assertEquals("0.25", body.getValue("temperature").jsonPrimitive.content)
    }
}
