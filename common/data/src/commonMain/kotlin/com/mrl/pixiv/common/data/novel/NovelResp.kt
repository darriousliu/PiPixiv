package com.mrl.pixiv.common.data.novel

import androidx.compose.runtime.Immutable
import com.mrl.pixiv.common.data.AiType
import com.mrl.pixiv.common.data.ImageUrls
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Tag
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
 * 热门小说标签响应
 * 注意:illust字段保留用于展示封面图
 */
@Serializable
@Immutable
data class TrendingNovelTagsResp(
    @SerialName("trend_tags")
    val trendTags: List<TrendNovelTag>
)

@Serializable
@Immutable
data class TrendNovelTag(
    val tag: String,

    @SerialName("translated_name")
    val translatedName: String? = null,

    // 注意:这里的illust是用于展示的封面图
    val illust: TrendNovelTagIllust
)

@Serializable
@Immutable
data class TrendNovelTagIllust(
    val id: Long,
    val title: String,
    val type: String,

    @SerialName("image_urls")
    val imageUrls: ImageUrls,

    val caption: String,
    val restrict: Long,
    val user: User,
    val tags: List<Tag>,
    val tools: List<String>,

    @SerialName("create_date")
    val createDate: String,

    @SerialName("page_count")
    val pageCount: Int,

    val width: Int,
    val height: Int,

    @SerialName("sanity_level")
    val sanityLevel: Int,

    @SerialName("x_restrict")
    val xRestrict: Int,

    @SerialName("total_view")
    val totalView: Long,

    @SerialName("total_bookmarks")
    val totalBookmarks: Long,

    @SerialName("is_bookmarked")
    val isBookmarked: Boolean,

    val visible: Boolean,

    @SerialName("is_muted")
    val isMuted: Boolean,

    @SerialName("illust_ai_type")
    val illustAiType: AiType,

    @SerialName("illust_book_style")
    val illustBookStyle: Int
)

/**
 * 小说文本内容响应
 * 从HTML中解析得到的小说正文
 */
@Serializable
@Immutable
data class NovelTextResp(
    val text: String,

    @SerialName("series_navigation")
    val seriesNavigation: SeriesNavigation? = null
)

@Serializable
@Immutable
data class SeriesNavigation(
    @SerialName("next_novel")
    val nextNovel: SeriesNovel? = null,

    @SerialName("prev_novel")
    val prevNovel: SeriesNovel? = null
)

@Serializable
@Immutable
data class SeriesNovel(
    val id: Long,
    val viewable: Boolean,

    @SerialName("content_order")
    val contentOrder: String,

    val title: String,

    @SerialName("cover_url")
    val coverUrl: String,

    @SerialName("viewable_message")
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
