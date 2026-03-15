package com.mrl.pixiv.common.data.novel

import androidx.compose.runtime.Immutable
import com.mrl.pixiv.common.data.AiType
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 小说系列详情响应
 */
@Serializable
@Immutable
data class NovelSeriesResp(
    @SerialName("novel_series_detail")
    val novelSeriesDetail: NovelSeriesDetail,

    @SerialName("novel_series_first_novel")
    val novelSeriesFirstNovel: Novel,

    @SerialName("novel_series_latest_novel")
    val novelSeriesLatestNovel: Novel,

    val novels: List<Novel>,

    @SerialName("next_url")
    val nextUrl: String? = null
)

/**
 * 小说系列详情
 */
@Serializable
@Immutable
data class NovelSeriesDetail(
    val id: Long,
    val title: String,
    val caption: String,

    @SerialName("is_original")
    val isOriginal: Boolean,

    @SerialName("is_concluded")
    val isConcluded: Boolean,

    @SerialName("content_count")
    val contentCount: Int,

    @SerialName("total_character_count")
    val totalCharacterCount: Long,

    val user: User,

    @SerialName("display_text")
    val displayText: String,

    @SerialName("novel_ai_type")
    val novelAiType: AiType,

    @SerialName("watchlist_added")
    val watchlistAdded: Boolean
)

/**
 * 小说详情响应
 */
@Serializable
@Immutable
data class NovelDetailResp(
    val novel: Novel
)

/**
 * 搜索小说响应
 */
@Serializable
@Immutable
data class SearchNovelResp(
    val novels: List<Novel>,

    @SerialName("next_url")
    val nextUrl: String? = null,

    @SerialName("search_span_limit")
    val searchSpanLimit: Long? = null
)


/**
 * 小说文本内容响应
 * 从HTML中解析得到的小说正文
 */
@Serializable
@Immutable
data class NovelTextResp(
    val text: String,
    val seriesNavigation: SeriesNavigation? = null
)

@Serializable
@Immutable
data class SeriesNavigation(
    val nextNovel: SeriesNovel? = null,
    val prevNovel: SeriesNovel? = null
)

@Serializable
@Immutable
data class SeriesNovel(
    val id: Long,
    val viewable: Boolean,
    val contentOrder: String,
    val title: String,
    val coverUrl: String,
    val viewableMessage: String? = null
)

/**
 * 小说推荐响应
 */
@Serializable
@Immutable
data class NovelRecommendedResp(
    val novels: List<Novel>,

    @SerialName("ranking_novels")
    val rankingNovels: List<Novel> = emptyList(),

    @SerialName("next_url")
    val nextUrl: String? = null
)

/**
 * 小说排行榜响应
 */
@Serializable
@Immutable
data class NovelRankingResp(
    val novels: List<Novel>,

    @SerialName("next_url")
    val nextUrl: String? = null
)

/**
 * 小说相关作品响应
 */
@Serializable
@Immutable
data class NovelRelatedResp(
    val novels: List<Novel>,

    @SerialName("next_url")
    val nextUrl: String? = null
)
