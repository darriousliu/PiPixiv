package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.Illust

object IllustCacheRepo {
    private val cache = mutableMapOf<String, List<Illust>>()

    operator fun get(key: String): List<Illust> = cache[key] ?: emptyList()

    operator fun set(key: String, list: List<Illust>) {
        cache[key] = list
    }

    fun removeList(key: String) {
        cache.remove(key)
    }
}