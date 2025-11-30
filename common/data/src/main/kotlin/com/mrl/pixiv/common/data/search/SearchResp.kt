package com.mrl.pixiv.common.data.search

import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Tag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchIllustResp(
    val illusts: List<Illust>,

    @SerialName("next_url")
    val nextUrl: String? = null,

    @SerialName("search_span_limit")
    val searchSpanLimit: Long,

    @SerialName("show_ai")
    val showAi: Boolean
)

@Serializable
data class SearchAutoCompleteResp(
    val tags: List<Tag>
)

@Serializable
data class TrendingTag(
    val illust: Illust,
    val tag: String,
    @SerialName("translated_name")
    val translatedName: String? = null
)

@Serializable
data class TrendingTagsResp(
    @SerialName("trend_tags")
    val trendTags: List<TrendingTag>
)