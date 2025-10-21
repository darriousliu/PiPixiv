package com.mrl.pixiv.common.serialize

import com.mrl.pixiv.common.network.JSON

inline fun <reified T> T.toJson() = JSON.encodeToString(this)

inline fun <reified T> String.fromJson() = JSON.decodeFromString<T>(this)