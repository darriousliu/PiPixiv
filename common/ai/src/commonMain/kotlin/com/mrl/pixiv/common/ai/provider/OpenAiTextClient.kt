package com.mrl.pixiv.common.ai.provider

import com.mrl.pixiv.common.ai.AiMessageRole
import com.mrl.pixiv.common.ai.AiTextRequest
import com.mrl.pixiv.common.ai.AiTextResponse
import com.mrl.pixiv.common.ai.internal.AI_CONNECT_TIMEOUT_MILLIS
import com.mrl.pixiv.common.ai.internal.AI_GENERATION_TIMEOUT_MILLIS
import com.mrl.pixiv.common.ai.internal.AiHttpClientHolder
import com.mrl.pixiv.common.ai.internal.jsonArrayOrNull
import com.mrl.pixiv.common.ai.internal.jsonObjectOrNull
import com.mrl.pixiv.common.ai.internal.normalizeBaseUrl
import com.mrl.pixiv.common.ai.internal.stringOrNull
import com.mrl.pixiv.common.ai.internal.toJsonObject
import com.mrl.pixiv.common.ai.internal.withExtraBody
import com.mrl.pixiv.common.ai.model.OpenAiApiType
import com.mrl.pixiv.common.data.setting.AiProvider
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.sse.serverSentEvents
import io.ktor.client.plugins.timeout
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.koin.core.annotation.Single

@Single
class OpenAiTextClient(
    private val httpClientHolder: AiHttpClientHolder,
) : AiTextProviderClient {
    override val provider: AiProvider = AiProvider.OPENAI

    override suspend fun generateText(request: AiTextRequest): AiTextResponse {
        val apiType =
            if (request.responseApi) OpenAiApiType.RESPONSES else OpenAiApiType.CHAT_COMPLETIONS
        val response = httpClientHolder.client.post {
            url(openAiUrl(request.endpoint, apiType))
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            if (request.apiKey.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer ${request.apiKey}")
            }
            setBody(
                when (apiType) {
                    OpenAiApiType.CHAT_COMPLETIONS -> buildChatCompletionsBody(request)
                        .withExtraBody(
                            extraBody = request.extraBody,
                            reservedKeys = setOf("model", "messages"),
                            providerName = provider.name,
                        )
                        .toString()

                    OpenAiApiType.RESPONSES -> buildResponsesBody(request)
                        .withExtraBody(
                            extraBody = request.extraBody,
                            reservedKeys = setOf("model", "input"),
                            providerName = provider.name,
                        )
                        .toString()
                }
            )
        }

        val payload = response.toJsonObject(providerName = provider.name)
        val text = when (apiType) {
            OpenAiApiType.CHAT_COMPLETIONS -> parseChatCompletionsText(payload)
            OpenAiApiType.RESPONSES -> parseResponsesText(payload)
        }.trim()

        require(text.isNotBlank()) {
            "AI returned empty text response."
        }
        return AiTextResponse(text = text)
    }

    override fun generateTextStream(request: AiTextRequest): Flow<AiTextStreamEvent> = flow {
        val apiType =
            if (request.responseApi) OpenAiApiType.RESPONSES else OpenAiApiType.CHAT_COMPLETIONS

        httpClientHolder.client.serverSentEvents(
            request = {
                method = HttpMethod.Post
                url(openAiUrl(request.endpoint, apiType))
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                if (request.apiKey.isNotBlank()) {
                    header(HttpHeaders.Authorization, "Bearer ${request.apiKey}")
                }
                timeout {
                    requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                    connectTimeoutMillis = AI_CONNECT_TIMEOUT_MILLIS
                    socketTimeoutMillis = AI_GENERATION_TIMEOUT_MILLIS
                }
                setBody(buildStreamingBody(request, apiType).toString())
            }
        ) {
            openAiStreamEvents(
                dataFrames = incoming.mapNotNull { it.data },
                apiType = apiType,
            ).collect { event ->
                this@flow.emit(event)
            }
        }
    }

    internal fun openAiUrl(endpoint: String, apiType: OpenAiApiType): String {
        val base = normalizeBaseUrl(endpoint)
        if (base.endsWith("/responses") || base.endsWith("/chat/completions")) return base

        val route = when (apiType) {
            OpenAiApiType.CHAT_COMPLETIONS -> "chat/completions"
            OpenAiApiType.RESPONSES -> "responses"
        }

        return if (base.endsWith("/v1")) "$base/$route" else "$base/v1/$route"
    }

    internal fun buildChatCompletionsBody(request: AiTextRequest): JsonObject {
        return buildJsonObject {
            put("model", request.model)
            putJsonArray("messages") {
                request.messages.forEach { message ->
                    add(
                        buildJsonObject {
                            put("role", message.role.toOpenAiRole())
                            put("content", message.content)
                        }
                    )
                }
            }
        }
    }

    internal fun buildResponsesBody(request: AiTextRequest): JsonObject {
        return buildJsonObject {
            put("model", request.model)
            put(
                "input",
                buildJsonArray {
                    request.messages.forEach { message ->
                        add(
                            buildJsonObject {
                                put("role", message.role.toOpenAiRole())
                                put(
                                    "content",
                                    buildJsonArray {
                                        add(
                                            buildJsonObject {
                                                put("type", "input_text")
                                                put("text", message.content)
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                }
            )
        }
    }

    internal fun buildStreamingBody(
        request: AiTextRequest,
        apiType: OpenAiApiType,
    ): JsonObject {
        val base = when (apiType) {
            OpenAiApiType.CHAT_COMPLETIONS -> buildChatCompletionsBody(request)
            OpenAiApiType.RESPONSES -> buildResponsesBody(request)
        }
        val reservedKeys = when (apiType) {
            OpenAiApiType.CHAT_COMPLETIONS -> setOf("model", "messages", "stream")
            OpenAiApiType.RESPONSES -> setOf("model", "input", "stream")
        }
        val merged = base.withExtraBody(
            extraBody = request.extraBody,
            reservedKeys = reservedKeys,
            providerName = provider.name,
        )
        return JsonObject(merged + ("stream" to JsonPrimitive(true)))
    }

    private fun parseChatCompletionsText(payload: JsonObject): String {
        val message = payload["choices"]?.jsonArrayOrNull()
            ?.firstOrNull()
            ?.jsonObjectOrNull()
            ?.get("message")
            ?.jsonObjectOrNull()
            ?: return ""
        return extractOpenAiMessageContent(message["content"])
    }

    private fun parseResponsesText(payload: JsonObject): String {
        val outputText = payload["output_text"].stringOrNull()
        if (!outputText.isNullOrBlank()) return outputText

        return payload["output"]?.jsonArrayOrNull()
            ?.joinToString(separator = "") { outputItem ->
                val item = outputItem.jsonObjectOrNull() ?: return@joinToString ""
                when (item["type"].stringOrNull()) {
                    "message" -> item["content"]?.jsonArrayOrNull()
                        ?.joinToString(separator = "") { contentPart ->
                            val part = contentPart.jsonObjectOrNull() ?: return@joinToString ""
                            part["text"].stringOrNull()
                                ?: part["value"].stringOrNull()
                                ?: ""
                        }
                        .orEmpty()

                    "output_text" -> item["text"].stringOrNull().orEmpty()
                    else -> ""
                }
            }
            .orEmpty()
    }

    private fun extractOpenAiMessageContent(content: JsonElement?): String {
        return when (content) {
            is JsonPrimitive -> content.contentOrNull.orEmpty()
            is JsonObject -> content["text"].stringOrNull().orEmpty()
            is JsonArray -> content.joinToString(separator = "") { part ->
                when (part) {
                    is JsonPrimitive -> part.contentOrNull.orEmpty()
                    is JsonObject -> part["text"].stringOrNull().orEmpty()
                    else -> ""
                }
            }

            else -> ""
        }
    }
}

internal data class OpenAiStreamFrame(
    val delta: String = "",
    val completed: Boolean = false,
    val error: String? = null,
)

internal fun openAiStreamEvents(
    dataFrames: Flow<String>,
    apiType: OpenAiApiType,
): Flow<AiTextStreamEvent> = flow {
    val accumulated = StringBuilder()
    var completed = false

    dataFrames.firstOrNull { data ->
        val frame = parseOpenAiStreamFrame(data, apiType)
        frame.error?.let { error ->
            throw IllegalStateException(error)
        }
        if (frame.delta.isNotEmpty()) {
            accumulated.append(frame.delta)
            emit(AiTextStreamEvent.Delta(frame.delta))
        }
        if (frame.completed) {
            val text = accumulated.toString().trim()
            require(text.isNotBlank()) {
                "AI returned empty text response."
            }
            completed = true
            emit(AiTextStreamEvent.Completed(text))
        }
        completed
    }

    check(completed) {
        "AI stream ended before a completion event."
    }
}

internal fun parseOpenAiStreamFrame(
    data: String,
    apiType: OpenAiApiType,
): OpenAiStreamFrame {
    if (data.trim() == "[DONE]") {
        return OpenAiStreamFrame(completed = true)
    }

    val payload = try {
        com.mrl.pixiv.common.ai.internal.aiJson.parseToJsonElement(data).jsonObjectOrNull()
    } catch (e: Exception) {
        return OpenAiStreamFrame(error = "Invalid OpenAI streaming event.")
    } ?: return OpenAiStreamFrame(error = "Invalid OpenAI streaming event.")

    payload["error"]?.jsonObjectOrNull()?.let { error ->
        return OpenAiStreamFrame(
            error = error["message"].stringOrNull() ?: "OpenAI streaming request failed."
        )
    }

    return when (apiType) {
        OpenAiApiType.CHAT_COMPLETIONS -> {
            val choice = payload["choices"]?.jsonArrayOrNull()?.firstOrNull()?.jsonObjectOrNull()
                ?: return OpenAiStreamFrame()
            val delta = choice["delta"]?.jsonObjectOrNull()
                ?.get("content")
                .let(::extractOpenAiStreamContent)
            val finishReason = choice["finish_reason"]
            OpenAiStreamFrame(
                delta = delta,
                completed = finishReason != null && finishReason !is JsonNull,
            )
        }

        OpenAiApiType.RESPONSES -> when (payload["type"].stringOrNull()) {
            "response.output_text.delta" -> OpenAiStreamFrame(
                delta = payload["delta"].stringOrNull().orEmpty()
            )

            "response.completed" -> OpenAiStreamFrame(completed = true)
            "response.failed", "response.incomplete", "error" -> OpenAiStreamFrame(
                error = payload["message"].stringOrNull()
                    ?: payload["response"]?.jsonObjectOrNull()
                        ?.get("error")?.jsonObjectOrNull()
                        ?.get("message").stringOrNull()
                    ?: "OpenAI streaming request failed."
            )

            else -> OpenAiStreamFrame()
        }
    }
}

private fun extractOpenAiStreamContent(content: JsonElement?): String = when (content) {
    is JsonPrimitive -> content.contentOrNull.orEmpty()
    is JsonObject -> content["text"].stringOrNull().orEmpty()
    is JsonArray -> content.joinToString(separator = "") { item ->
        when (item) {
            is JsonPrimitive -> item.contentOrNull.orEmpty()
            is JsonObject -> item["text"].stringOrNull().orEmpty()
            else -> ""
        }
    }

    else -> ""
}

private fun AiMessageRole.toOpenAiRole(): String = when (this) {
    AiMessageRole.SYSTEM -> "system"
    AiMessageRole.USER -> "user"
    AiMessageRole.ASSISTANT -> "assistant"
}
