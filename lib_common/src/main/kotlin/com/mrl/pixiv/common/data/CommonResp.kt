package com.mrl.pixiv.common.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement


@Serializable
@Immutable
class EmptyResp

@Serializable
@Immutable
data class ProfileImageUrls(
    val medium: String = ""
)

@Serializable
@Immutable
data class ImageUrls(
    @SerialName("square_medium")
    val squareMedium: String = "",

    val medium: String = "",
    val large: String = "",
    val original: String = ""
)

@Serializable
@Immutable
data class Illust(
    val id: Long,
    val title: String,
    val type: Type,

    @SerialName("image_urls")
    val imageUrls: ImageUrls,

    val caption: String = "",
    val restrict: Long = 0,
    val user: User,
    val tags: List<Tag>? = null,
    val tools: List<String>? = null,

    @SerialName("create_date")
    val createDate: String = "",

    @SerialName("page_count")
    val pageCount: Int = 0,

    val width: Int,
    val height: Int,

    @SerialName("sanity_level")
    val sanityLevel: Int = 0,

    @SerialName("x_restrict")
    val xRestrict: Int = 0,

    @Transient
    val series: JsonElement? = null,

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

    val visible: Boolean? = null,

    @SerialName("is_muted")
    val isMuted: Boolean? = null,

    @SerialName("illust_ai_type")
    val illustAIType: IllustAiType = IllustAiType.Undefined,

    @SerialName("illust_book_style")
    val illustBookStyle: Int = 0,
)

@Serializable
@Immutable
data class MetaPage(
    @SerialName("image_urls")
    val imageUrls: ImageUrls? = null
)

@Serializable
@Immutable
data class MetaSinglePage(
    @SerialName("original_image_url")
    val originalImageURL: String = ""
)

@Serializable
@Immutable
data class Tag(
    val name: String = "",

    @SerialName("translated_name")
    val translatedName: String = "",

    @SerialName("added_by_uploaded_user")
    val addedByUploadedUser: Boolean = false
)

@Serializable
@Immutable
enum class Type(val value: String) {
    @SerialName("illust")
    Illust("illust"),

    @SerialName("manga")
    Manga("manga"),

    @SerialName("ugoira")
    Ugoira("ugoira");
}

@Serializable
@Immutable
data class User(
    val id: Long = 0,
    val name: String = "",
    val account: String = "",

    @SerialName("profile_image_urls")
    val profileImageUrls: ProfileImageUrls = ProfileImageUrls(),

    val comment: String = "",

    @SerialName("is_followed")
    val isFollowed: Boolean = false,

    @SerialName("is_access_blocking_user")
    val isAccessBlockingUser: Boolean = false,

    @SerialName("is_accept_request")
    val isAcceptRequest: Boolean = false,
)

@Serializable
@Immutable
data class Novel(
    val id: Long,
    val title: String,
    val caption: String,
    val restrict: Long,

    @SerialName("x_restrict")
    val xRestrict: Long,

    @SerialName("is_original")
    val isOriginal: Boolean,

    @SerialName("image_urls")
    val imageUrls: ImageUrls,

    @SerialName("create_date")
    val createDate: String,

    val tags: List<Tag>,

    @SerialName("page_count")
    val pageCount: Long,

    @SerialName("text_length")
    val textLength: Long,

    val user: User,
    val series: Series,

    @SerialName("is_bookmarked")
    val isBookmarked: Boolean,

    @SerialName("total_bookmarks")
    val totalBookmarks: Long,

    @SerialName("total_view")
    val totalView: Long,

    val visible: Boolean,

    @SerialName("total_comments")
    val totalComments: Long,

    @SerialName("is_muted")
    val isMuted: Boolean,

    @SerialName("is_mypixiv_only")
    val isMypixivOnly: Boolean,

    @SerialName("is_x_restricted")
    val isXRestricted: Boolean,

    @SerialName("novel_ai_type")
    val novelAiType: Long
)


@Serializable
@Immutable
data class Series(
    val id: Long? = null,
    val title: String? = null
)

@Serializable
@Immutable
enum class IllustAiType(val value: Int) {
    @SerialName("0")
    Undefined(0),

    @SerialName("1")
    NotAiGeneratedWork(1),

    @SerialName("2")
    AiGeneratedWorks(2)
}