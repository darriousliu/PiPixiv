package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.novel.NovelTextResp
import kotlinx.serialization.json.Json

object NovelContentParser {
    private val novelDataRegex =
        """(?s)novel:\s*(\{.*?\})\s*,\s*isOwnWork:""".toRegex()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun extract(html: String): NovelTextResp? {
        val novelJson = novelDataRegex.find(html)?.groupValues?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return runCatching {
            json.decodeFromString<NovelTextResp>(novelJson)
        }.getOrNull()
    }
}
