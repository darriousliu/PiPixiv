package com.mrl.pixiv.common.data.search

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.user.UserPreview
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
    val showAi: Boolean = true
)

@Serializable
data class SearchUserResp(
    @SerialName("user_previews")
    val userPreviews: List<UserPreview>,
    @SerialName("next_url")
    val nextUrl: String? = null
)

@Serializable
data class SearchAutoCompleteResp(
    val tags: List<Tag>
)

@Stable
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
