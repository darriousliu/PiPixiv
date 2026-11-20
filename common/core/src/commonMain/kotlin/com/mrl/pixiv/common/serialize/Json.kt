package com.mrl.pixiv.common.serialize

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
val JSON = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    exceptionsWithDebugInfo = false
}

inline fun <reified T> T.toJson() = JSON.encodeToString(this)

inline fun <reified T> String.fromJson() = JSON.decodeFromString<T>(this)
