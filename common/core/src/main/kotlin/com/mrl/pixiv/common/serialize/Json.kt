package com.mrl.pixiv.common.serialize

import kotlinx.serialization.json.Json

val JSON = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

inline fun <reified T> T.toJson() = JSON.encodeToString(this)

inline fun <reified T> String.fromJson() = JSON.decodeFromString<T>(this)