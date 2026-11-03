package com.mrl.pixiv.common.data.manga

import androidx.compose.runtime.Immutable
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.ImageUrls
import com.mrl.pixiv.common.data.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class MangaSeriesDetail(
    val id: Long,
    val title: String = "",
    val caption: String = "",
    @SerialName("cover_image_urls") val coverImageUrls: ImageUrls = ImageUrls(),
    @SerialName("create_date") val createDate: String = "",
    @SerialName("series_work_count") val seriesWorkCount: Int = 0,
    val user: User = User(),
    @SerialName("watchlist_added") val watchlistAdded: Boolean = false,
)

@Serializable
@Immutable
data class MangaSeriesResp(
    @SerialName("illust_series_detail") val detail: MangaSeriesDetail,
    @SerialName("illust_series_first_illust") val firstIllust: Illust? = null,
    val illusts: List<Illust> = emptyList(),
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
@Immutable
data class MangaSeriesContext(
    @SerialName("content_order") val contentOrder: Int = 0,
    val next: Illust? = null,
    val prev: Illust? = null,
)

@Serializable
@Immutable
data class MangaSeriesContextResp(
    @SerialName("illust_series_detail") val detail: MangaSeriesDetail? = null,
    @SerialName("illust_series_context") val context: MangaSeriesContext? = null,
)

@Serializable
@Immutable
data class UserMangaSeriesResp(
    @SerialName("illust_series_details") val series: List<MangaSeriesDetail> = emptyList(),
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
@Immutable
data class MangaWatchlistResp(
    val series: List<MangaWatchlistEntry?>? = emptyList(),
    @SerialName("next_url") val nextUrl: String? = null,
)

/** Restricted entries may contain only mask_text; never infer navigation IDs for them. */
@Serializable
@Immutable
data class MangaWatchlistEntry(
    val id: Long? = null,
    val title: String? = null,
    val user: User? = null,
    val url: String? = null,
    @SerialName("published_content_count") val publishedContentCount: Int? = null,
    @SerialName("last_published_content_datetime") val lastPublishedContentDatetime: String? = null,
    @SerialName("latest_content_id") val latestContentId: Long? = null,
    @SerialName("mask_text") val maskText: String? = null,
) {
    val isMasked: Boolean get() = !maskText.isNullOrBlank()
    val navigableSeriesId: Long? get() = id?.takeIf { it > 0 && !isMasked }
    val navigableLatestId: Long? get() = latestContentId?.takeIf { it > 0 && !isMasked }
}
