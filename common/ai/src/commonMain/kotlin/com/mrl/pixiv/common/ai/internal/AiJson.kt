package com.mrl.pixiv.common.ai.internal

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal val aiJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

internal suspend fun HttpResponse.toJsonObject(providerName: String): JsonObject {
    val raw = bodyAsText()
    if (!status.isSuccess()) {
        val errorMessage = extractErrorMessage(raw)
        throw IllegalStateException("$providerName request failed (${status.value}): $errorMessage")
    }

    val element = try {
        aiJson.parseToJsonElement(raw)
    } catch (e: Exception) {
        throw IllegalStateException(
            "$providerName returned non-JSON response: ${raw.take(MAX_ERROR_BODY_LENGTH)}",
            e,
        )
    }

    return element.jsonObjectOrNull()
        ?: throw IllegalStateException("$providerName returned a non-object JSON payload")
}

internal fun normalizeBaseUrl(endpoint: String): String {
    val trimmed = endpoint.trim()
    return if (trimmed.endsWith('/')) trimmed.dropLast(1) else trimmed
}

private fun extractErrorMessage(raw: String): String {
    val parsed = runCatching { aiJson.parseToJsonElement(raw) }.getOrNull()
    val objectPayload = parsed.jsonObjectOrNull() ?: return raw.take(MAX_ERROR_BODY_LENGTH)

    val errorNode = objectPayload["error"]
    val fromObject = errorNode.jsonObjectOrNull()?.get("message")?.stringOrNull()
    if (!fromObject.isNullOrBlank()) return fromObject

    val fromPrimitive = errorNode.stringOrNull()
    if (!fromPrimitive.isNullOrBlank()) return fromPrimitive

    return raw.take(MAX_ERROR_BODY_LENGTH)
}

private const val MAX_ERROR_BODY_LENGTH = 500

internal fun JsonElement?.jsonObjectOrNull(): JsonObject? = this as? JsonObject

internal fun JsonElement?.jsonArrayOrNull(): JsonArray? = this as? JsonArray

internal fun JsonElement?.stringOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull
