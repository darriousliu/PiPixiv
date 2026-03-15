package com.mrl.pixiv.common.data.search

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.data.AiType
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.ImageUrls
import com.mrl.pixiv.common.data.MetaPage
import com.mrl.pixiv.common.data.MetaSinglePage
import com.mrl.pixiv.common.data.Series
import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.User
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
    val illust: TrendTagIllust,
    val tag: String,
    @SerialName("translated_name")
    val translatedName: String? = null
)

@Serializable
data class TrendingTagsResp(
    @SerialName("trend_tags")
    val trendTags: List<TrendingTag>
)

@Serializable
@Immutable
data class TrendTagIllust(
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

    val series: Series? = null,

    @SerialName("meta_single_page")
    val metaSinglePage: MetaSinglePage,

    @SerialName("meta_pages")
    val metaPages: List<MetaPage>? = null,

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