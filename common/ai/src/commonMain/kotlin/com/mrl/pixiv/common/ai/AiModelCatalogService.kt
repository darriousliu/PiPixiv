package com.mrl.pixiv.common.ai

import com.mrl.pixiv.common.ai.internal.AiHttpClientHolder
import com.mrl.pixiv.common.ai.internal.jsonArrayOrNull
import com.mrl.pixiv.common.ai.internal.jsonObjectOrNull
import com.mrl.pixiv.common.ai.internal.normalizeBaseUrl
import com.mrl.pixiv.common.ai.internal.stringOrNull
import com.mrl.pixiv.common.ai.internal.toJsonObject
import com.mrl.pixiv.common.data.setting.AiProvider
import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.koin.core.annotation.Single

@Single
class AiModelCatalogService(
    private val httpClientHolder: AiHttpClientHolder,
) {
    suspend fun fetchModels(config: AiTranslationConfig): List<String> {
        return fetchModels(config, httpClientHolder.client)
    }

    internal suspend fun fetchModels(
        config: AiTranslationConfig,
        httpClient: HttpClient,
    ): List<String> {
        val apiKey = config.apiKey.trim()
        val response = httpClient.get {
            url(modelCatalogUrl(config.provider, config.endpoint))
            when (config.provider) {
                AiProvider.OPENAI -> {
                    if (apiKey.isNotEmpty()) {
                        header(HttpHeaders.Authorization, "Bearer $apiKey")
                    }
                }

                AiProvider.CLAUDE -> {
                    if (apiKey.isNotEmpty()) {
                        header(CLAUDE_API_KEY_HEADER, apiKey)
                    }
                    header(CLAUDE_VERSION_HEADER, CLAUDE_API_VERSION)
                }

                AiProvider.GEMINI -> {
                    if (apiKey.isNotEmpty()) {
                        header(GEMINI_API_KEY_HEADER, apiKey)
                    }
                }
            }
        }
        val payload = response.toJsonObject(providerName = config.provider.name)

        return when (config.provider) {
            AiProvider.OPENAI,
            AiProvider.CLAUDE -> payload.requireModelArray(
                key = DATA_KEY,
                providerName = config.provider.name,
            ).modelIds(ID_KEY)

            AiProvider.GEMINI -> payload.requireModelArray(
                key = MODELS_KEY,
                providerName = config.provider.name,
            ).modelIds(NAME_KEY) { modelId ->
                modelId.removePrefix(GEMINI_MODEL_PREFIX)
            }
        }
    }
}

private fun modelCatalogUrl(
    provider: AiProvider,
    endpoint: String,
): String {
    val base = normalizeBaseUrl(endpoint)
    if (base.endsWith(MODELS_PATH)) return base

    val version = when (provider) {
        AiProvider.OPENAI,
        AiProvider.CLAUDE -> V1_PATH

        AiProvider.GEMINI -> V1_BETA_PATH
    }
    return if (base.endsWith(version)) {
        "$base$MODELS_PATH"
    } else {
        "$base$version$MODELS_PATH"
    }
}

private fun JsonObject.requireModelArray(
    key: String,
    providerName: String,
): JsonArray {
    return get(key)?.jsonArrayOrNull()
        ?: throw IllegalStateException(
            "$providerName returned a models response without a '$key' array"
        )
}

private fun JsonArray.modelIds(
    key: String,
    transform: (String) -> String = { it },
): List<String> {
    return mapNotNull { item ->
        item.jsonObjectOrNull()
            ?.get(key)
            ?.stringOrNull()
            ?.trim()
            ?.let(transform)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }.distinct()
}

private const val DATA_KEY = "data"
private const val MODELS_KEY = "models"
private const val ID_KEY = "id"
private const val NAME_KEY = "name"
private const val MODELS_PATH = "/models"
private const val V1_PATH = "/v1"
private const val V1_BETA_PATH = "/v1beta"
private const val GEMINI_MODEL_PREFIX = "models/"
private const val CLAUDE_API_KEY_HEADER = "x-api-key"
private const val CLAUDE_VERSION_HEADER = "anthropic-version"
private const val CLAUDE_API_VERSION = "2023-06-01"
private const val GEMINI_API_KEY_HEADER = "x-goog-api-key"
