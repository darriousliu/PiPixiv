package com.mrl.pixiv.common.ai.provider

import com.mrl.pixiv.common.ai.AiMessageRole
import com.mrl.pixiv.common.ai.AiTextRequest
import com.mrl.pixiv.common.ai.AiTextResponse
import com.mrl.pixiv.common.ai.internal.AiHttpClientHolder
import com.mrl.pixiv.common.ai.internal.jsonArrayOrNull
import com.mrl.pixiv.common.ai.internal.jsonObjectOrNull
import com.mrl.pixiv.common.ai.internal.normalizeBaseUrl
import com.mrl.pixiv.common.ai.internal.stringOrNull
import com.mrl.pixiv.common.ai.internal.toJsonObject
import com.mrl.pixiv.common.ai.model.OpenAiApiType
import com.mrl.pixiv.common.data.setting.AiProvider
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
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
                    OpenAiApiType.CHAT_COMPLETIONS -> buildChatCompletionsBody(request).toString()
                    OpenAiApiType.RESPONSES -> buildResponsesBody(request).toString()
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

    private fun openAiUrl(endpoint: String, apiType: OpenAiApiType): String {
        val base = normalizeBaseUrl(endpoint)
        if (base.endsWith("/responses") || base.endsWith("/chat/completions")) return base

        val route = when (apiType) {
            OpenAiApiType.CHAT_COMPLETIONS -> "chat/completions"
            OpenAiApiType.RESPONSES -> "responses"
        }

        return if (base.endsWith("/v1")) "$base/$route" else "$base/v1/$route"
    }

    private fun buildChatCompletionsBody(request: AiTextRequest): JsonObject {
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

    private fun buildResponsesBody(request: AiTextRequest): JsonObject {
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

private fun AiMessageRole.toOpenAiRole(): String = when (this) {
    AiMessageRole.SYSTEM -> "system"
    AiMessageRole.USER -> "user"
    AiMessageRole.ASSISTANT -> "assistant"
}
