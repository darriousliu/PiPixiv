package com.mrl.pixiv.common.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlin.jvm.JvmInline


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

/**
 * 表示插画、漫画或动图作品的类。
 *
 * @property id 作品的唯一标识符。
 * @property title 作品的标题。
 * @property type 作品的类型，可以是插画、漫画或动图。
 * @property imageUrls 作品关联的多分辨率图像URL。
 * @property caption 作品的描述或说明文字，默认为空字符串。
 * @property restrict 作品的访问限制级别，0表示公开。
 * @property user 发布作品的用户信息。
 * @property tags 关联的标签列表，可选属性。
 * @property tools 使用的创作工具列表，可选属性。
 * @property createDate 作品的创建日期，格式为ISO 8601，默认为空字符串。
 * @property pageCount 作品的页数，通常用于漫画或多页面的插画。
 * @property width 作品的宽度（像素）。
 * @property height 作品的高度（像素）。
 * @property sanityLevel 作品的内容敏感度分级，通常用于内容筛选，越高越令人不适。
 * @property xRestrict 作品的额外访问限制级别，0-普通，1-R18，2-R18G。
 * @property series 作品所属系列的元数据，可选属性。
 * @property metaSinglePage 单页作品的元数据，包含原始图像URL。
 * @property metaPages 多页作品的元数据列表，可选属性。
 * @property totalView 作品的总查看次数。
 * @property totalBookmarks 作品的总收藏次数。
 * @property isBookmarked 当前用户是否已收藏该作品。
 * @property visible 作品是否可见，可选属性。
 * @property isMuted 当前用户是否屏蔽该作品，可选属性。
 * @property illustAIType 作品是否为AI生成的标识。
 * @property illustBookStyle 作品的书籍风格标识，默认为0。
 */
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
    val xRestrict: XRestrict = XRestrict.Normal,

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

    @SerialName("total_comments")
    val totalComments: Int? = null,

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
    val xRestrict: XRestrict,

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
    val totalComments: Int? = null,

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

@JvmInline
@Serializable
value class XRestrict(val value: Int) {
    companion object {
        val Normal = XRestrict(0)
        val R18 = XRestrict(1)
        val R18G = XRestrict(2)
    }
}