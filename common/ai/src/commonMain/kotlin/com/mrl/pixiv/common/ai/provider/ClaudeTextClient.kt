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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.koin.core.annotation.Single

@Single
class ClaudeTextClient(
    private val httpClientHolder: AiHttpClientHolder,
) : AiTextProviderClient {
    override val provider: AiProvider = AiProvider.CLAUDE

    override suspend fun generateText(request: AiTextRequest): AiTextResponse {
        val systemPrompt = request.messages
            .filter { it.role == AiMessageRole.SYSTEM }
            .joinToString(separator = "\n\n") { it.content }
            .trim()

        val messages = request.messages.filter { it.role != AiMessageRole.SYSTEM }
        require(messages.isNotEmpty()) {
            "Claude request requires at least one non-system message."
        }

        val resolvedModel = request.model
        val response = httpClientHolder.client.post {
            url(claudeUrl(request.endpoint))
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header("anthropic-version", ANTHROPIC_VERSION)
            if (request.apiKey.isNotBlank()) {
                header("x-api-key", request.apiKey)
            }
            setBody(
                buildJsonObject {
                    put("model", resolvedModel)
                    put("max_tokens", request.maxOutputTokens ?: DEFAULT_MAX_OUTPUT_TOKENS)
                    if (systemPrompt.isNotBlank()) {
                        put("system", systemPrompt)
                    }
                    putJsonArray("messages") {
                        messages.forEach { message ->
                            add(
                                buildJsonObject {
                                    put("role", message.role.toClaudeRole())
                                    put("content", message.content)
                                }
                            )
                        }
                    }
                }.toString()
            )
        }

        val payload = response.toJsonObject(providerName = provider.name)
        val text = payload["content"]?.jsonArrayOrNull()
            ?.mapNotNull { item ->
                val obj = item.jsonObjectOrNull() ?: return@mapNotNull null
                if (obj["type"].stringOrNull() != "text") return@mapNotNull null
                obj["text"].stringOrNull()
            }
            ?.joinToString(separator = "")
            ?.trim()
            .orEmpty()

        require(text.isNotBlank()) {
            "AI returned empty text response."
        }
        return AiTextResponse(text = text)
    }

    private fun claudeUrl(endpoint: String): String {
        val base = normalizeBaseUrl(endpoint)
        return when {
            base.endsWith("/messages") -> base
            base.endsWith("/v1") -> "$base/messages"
            else -> "$base/v1/messages"
        }
    }

    private companion object {
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private const val DEFAULT_MAX_OUTPUT_TOKENS = 64 * 1024
    }
}

private fun AiMessageRole.toClaudeRole(): String = when (this) {
    AiMessageRole.SYSTEM -> "user"
    AiMessageRole.USER -> "user"
    AiMessageRole.ASSISTANT -> "assistant"
}
