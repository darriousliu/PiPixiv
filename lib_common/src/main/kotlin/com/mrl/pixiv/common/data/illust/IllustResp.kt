package com.mrl.pixiv.common.data.illust

import com.mrl.pixiv.common.data.Illust
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IllustRecommendedResp(
    val illusts: List<Illust>,

    @SerialName("ranking_illusts")
    val rankingIllusts: List<Illust>,

    @SerialName("contest_exists")
    val contestExists: Boolean? = null,

    @SerialName("privacy_policy")
    val privacyPolicy: PrivacyPolicy,

    @SerialName("next_url")
    val nextURL: String? = null,
)


@Serializable
data class PrivacyPolicy(
    val version: String = "",
    val message: String = "",
    val url: String = ""
)

@Serializable
data class IllustDetailResp(
    val illust: Illust,
)

@Serializable
data class IllustBookmarkDetailResp(
    @SerialName("bookmark_detail")
    val bookmarkDetail: BookmarkDetail
)

@Serializable
data class BookmarkDetail(
    @SerialName("is_bookmarked")
    val isBookmarked: Boolean,
    val restrict: String,
    val tags: List<BookmarkDetailTag>
)

@Serializable
data class BookmarkDetailTag(
    @SerialName("is_registered")
    val isRegistered: Boolean,
    val name: String
)