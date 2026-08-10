package com.mrl.pixiv.common.router

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.Type
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@Stable
sealed class Destination : NavKey {
    @Serializable
    data object LoginOption : Destination()

    @Serializable
    data class Login(
        val startUrl: String,
    ) : Destination()

    @Serializable
    data object OAuthLogin : Destination()

    @Serializable
    data object WebCookieLogin: Destination()

    @Serializable
    data object Main : Destination()

    @Serializable
    data class ProfileDetail(
        val userId: Long,
    ) : Destination()

    @Serializable
    data class Picture(
        val index: Int,
        val prefix: String,
        val enableTransition: Boolean,
    ) : Destination()

    @Serializable
    data class PictureDeeplink(
        val illustId: Long,
    ) : Destination()

    @Serializable
    data class ImagePreview(
        val imageUrls: List<String>,
        val initialIndex: Int,
        val sharedElementKey: String? = null,
    ) : Destination()

    @Serializable
    data object Search : Destination()

    @Serializable
    data class SearchResults(
        val searchWords: String,
        val isIdSearch: Boolean = false,
        val searchMode: AppViewMode = AppViewMode.ILLUST,
    ) : Destination()

    @Serializable
    data object Setting : Destination()

    @Serializable
    data object NetworkSetting : Destination()

    @Serializable
    data object BrowsingSetting : Destination()

    @Serializable
    data object SearchSetting : Destination()

    @Serializable
    data object HistorySetting : Destination()

    @Serializable
    data object PrivacySetting : Destination()

    @Serializable
    data object FileNameFormat : Destination()

    @Serializable
    data object AiTranslationSetting : Destination()

    @Serializable
    data object History : Destination()

    @Serializable
    data object NovelReadLater : Destination()

    @Serializable
    data class Collection(
        val userId: Long,
        val isNovel: Boolean,
    ) : Destination()

    @Serializable
    data object BookmarkedTags : Destination()

    @Serializable
    data object NovelMarkers : Destination()

    @Serializable
    data class Following(
        val userId: Long,
    ) : Destination()

    @Serializable
    data class UserArtwork(
        val userId: Long,
        val initialType: Type = Type.Illust,
    ) : Destination()

    @Serializable
    data class UserNovels(
        val userId: Long,
    ) : Destination()

    @Serializable
    data object BlockSettings : Destination()

    @Serializable
    data object BlockIllust : Destination()

    @Serializable
    data object BlockNovel : Destination()

    @Serializable
    data object BlockUser : Destination()

    @Serializable
    data object BlockTag : Destination()

    @Serializable
    data object BlockComments : Destination()

    @Serializable
    data object AppData : Destination()

    @Serializable
    data object Download : Destination()

    @Serializable
    data object About : Destination()

    @Serializable
    data class Comment(val id: Long, val type: CommentType) : Destination()

    @Serializable
    data class Report(val id: Long, val type: ReportType) : Destination()

    @Serializable
    data class NovelDetail(
        val novelId: Long,
        val markerPage: Int? = null,
        val readLaterTargetLanguage: String? = null,
    ) : Destination()

    @Serializable
    data class NovelSeries(
        val seriesId: Long,
    ) : Destination()
}

@Serializable
sealed class MainPage(
    @Transient
    val icon: ImageVector = Icons.Rounded.Home,
) {
    @Serializable
    data object Home : MainPage(
        icon = Icons.Rounded.Home
    )

    @Serializable
    data object Ranking : MainPage(
        icon = Icons.Rounded.Equalizer
    )

    @Serializable
    data object Latest : MainPage(
        icon = Icons.Rounded.Favorite
    )

    @Serializable
    data object Search : MainPage(
        icon = Icons.Rounded.Search
    )

    @Serializable
    data object Profile : MainPage(
        icon = Icons.Rounded.AccountCircle
    )
}

@Serializable
enum class CommentType {
    ILLUST,
    NOVEL,
}

@Serializable
enum class ReportType {
    USER,
    ILLUST,
    NOVEL,
    ILLUST_COMMENT,
    NOVEL_COMMENT,
}
