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
import com.mrl.pixiv.common.data.setting.AiProvider
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.path
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.koin.core.annotation.Single

@Single
class GeminiTextClient(
    private val httpClientHolder: AiHttpClientHolder,
) : AiTextProviderClient {
    override val provider: AiProvider = AiProvider.GEMINI

    override suspend fun generateText(request: AiTextRequest): AiTextResponse {
        val systemPrompt = request.messages
            .filter { it.role == AiMessageRole.SYSTEM }
            .joinToString(separator = "\n\n") { it.content }
            .trim()

        val messages = request.messages.filter { it.role != AiMessageRole.SYSTEM }
        require(messages.isNotEmpty()) {
            "Gemini request requires at least one non-system message."
        }

        val resolvedModel = request.model
        val response = httpClientHolder.client.post {
            url(geminiUrl(request.endpoint, resolvedModel))
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header("x-goog-api-key", request.apiKey)
            setBody(
                buildJsonObject {
                    if (systemPrompt.isNotBlank()) {
                        put(
                            "system_instruction",
                            buildJsonObject {
                                put(
                                    "parts",
                                    buildJsonArray {
                                        add(buildJsonObject { put("text", systemPrompt) })
                                    }
                                )
                            }
                        )
                    }
                    putJsonArray("contents") {
                        messages.forEach { message ->
                            add(
                                buildJsonObject {
                                    put("role", message.role.toGeminiRole())
                                    put(
                                        "parts",
                                        buildJsonArray {
                                            add(buildJsonObject { put("text", message.content) })
                                        }
                                    )
                                }
                            )
                        }
                    }
                    request.maxOutputTokens?.let { maxOutputTokens ->
                        put(
                            "generationConfig",
                            buildJsonObject {
                                put("maxOutputTokens", maxOutputTokens)
                            }
                        )
                    }
                }.toString()
            )
        }

        val payload = response.toJsonObject(providerName = provider.name)
        val text = payload["candidates"]?.jsonArrayOrNull()
            ?.firstOrNull()?.jsonObjectOrNull()?.get("content")
            ?.jsonObjectOrNull()?.get("parts")
            ?.jsonArrayOrNull()?.mapNotNull { part ->
                part.jsonObjectOrNull()?.get("text")?.stringOrNull()
            }
            ?.joinToString(separator = "")
            ?.trim()
            .orEmpty()

        require(text.isNotBlank()) {
            "AI returned empty text response."
        }
        return AiTextResponse(text = text)
    }

    private fun geminiUrl(endpoint: String, model: String): String {
        val base = normalizeBaseUrl(endpoint)
        val versionedBase = URLBuilder(base).apply {
            path("v1beta")
        }
        val modelPath = if (model.startsWith("models/")) model else "models/$model"
        return "$versionedBase/$modelPath:generateContent"
    }
}

private fun AiMessageRole.toGeminiRole(): String = when (this) {
    AiMessageRole.SYSTEM -> "user"
    AiMessageRole.USER -> "user"
    AiMessageRole.ASSISTANT -> "model"
}
