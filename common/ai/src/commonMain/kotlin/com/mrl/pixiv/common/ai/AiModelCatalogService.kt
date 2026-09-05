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
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
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
        val catalogUrl = modelCatalogUrl(config.provider, config.endpoint)
        val models = linkedSetOf<String>()
        val seenCursors = mutableSetOf<String>()
        var cursor: String? = null

        while (true) {
            currentCoroutineContext().ensureActive()
            val response = httpClient.get {
                url(catalogUrl)
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
                        cursor?.let { parameter(CLAUDE_AFTER_ID_PARAMETER, it) }
                    }

                    AiProvider.GEMINI -> {
                        if (apiKey.isNotEmpty()) {
                            header(GEMINI_API_KEY_HEADER, apiKey)
                        }
                        cursor?.let { parameter(GEMINI_PAGE_TOKEN_PARAMETER, it) }
                    }
                }
            }
            val payload = response.toJsonObject(providerName = config.provider.name)

            models += when (config.provider) {
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

            val nextCursor = payload.nextModelPageCursor(config.provider) ?: break
            check(seenCursors.add(nextCursor)) {
                "${config.provider.name} returned a repeated models pagination cursor."
            }
            cursor = nextCursor
        }
        return models.toList()
    }
}

private fun JsonObject.nextModelPageCursor(provider: AiProvider): String? = when (provider) {
    AiProvider.OPENAI -> null
    AiProvider.CLAUDE -> {
        if ((get(CLAUDE_HAS_MORE_KEY) as? JsonPrimitive)?.booleanOrNull == true) {
            get(CLAUDE_LAST_ID_KEY)?.stringOrNull()?.takeIf(String::isNotBlank)
                ?: throw IllegalStateException(
                    "CLAUDE returned more models without a pagination cursor."
                )
        } else {
            null
        }
    }

    AiProvider.GEMINI -> get(GEMINI_NEXT_PAGE_TOKEN_KEY)
        ?.stringOrNull()
        ?.takeIf(String::isNotBlank)
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
private const val CLAUDE_HAS_MORE_KEY = "has_more"
private const val CLAUDE_LAST_ID_KEY = "last_id"
private const val CLAUDE_AFTER_ID_PARAMETER = "after_id"
private const val GEMINI_API_KEY_HEADER = "x-goog-api-key"
private const val GEMINI_NEXT_PAGE_TOKEN_KEY = "nextPageToken"
private const val GEMINI_PAGE_TOKEN_PARAMETER = "pageToken"
