package com.mrl.pixiv.common.ai.internal

import kotlinx.serialization.json.JsonObject

internal fun JsonObject.withExtraBody(
    extraBody: String,
    reservedKeys: Set<String>,
    providerName: String,
): JsonObject {
    val extra = parseExtraBody(extraBody, providerName)
    if (extra.isEmpty()) return this
    return mergeJsonObject(
        base = this,
        extra = extra,
        reservedKeys = reservedKeys,
    )
}

private fun parseExtraBody(extraBody: String, providerName: String): JsonObject {
    val trimmed = extraBody.trim()
    if (trimmed.isEmpty()) return JsonObject(emptyMap())

    val element = try {
        aiJson.parseToJsonElement(trimmed)
    } catch (e: Exception) {
        throw IllegalStateException("$providerName extra_body must be a valid JSON object.", e)
    }

    val root = element.jsonObjectOrNull()
        ?: throw IllegalStateException("$providerName extra_body must be a JSON object.")

    val openAiSdkExtra = root["extra_body"].jsonObjectOrNull()
    if (openAiSdkExtra == null) return root

    val remaining = root.filterKeys { it != "extra_body" }
    return mergeJsonObject(
        base = JsonObject(remaining),
        extra = openAiSdkExtra,
        reservedKeys = emptySet(),
    )
}

private fun mergeJsonObject(
    base: JsonObject,
    extra: JsonObject,
    reservedKeys: Set<String>,
): JsonObject {
    val merged = base.toMutableMap()
    extra.forEach { (key, value) ->
        if (key in reservedKeys) return@forEach
        val baseValue = merged[key]
        merged[key] = if (baseValue is JsonObject && value is JsonObject) {
            mergeJsonObject(
                base = baseValue,
                extra = value,
                reservedKeys = emptySet(),
            )
        } else {
            value
        }
    }
    return JsonObject(merged)
}
