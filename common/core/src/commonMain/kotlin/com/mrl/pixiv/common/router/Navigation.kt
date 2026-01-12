package com.mrl.pixiv.common.router

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey
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
    data object Search : Destination()

    @Serializable
    data class SearchResults(
        val searchWords: String,
        val isIdSearch: Boolean = false,
    ) : Destination()

    @Serializable
    data object Setting : Destination()

    @Serializable
    data object NetworkSetting : Destination()

    @Serializable
    data object FileNameFormat : Destination()

    @Serializable
    data object History : Destination()

    @Serializable
    data class Collection(
        val userId: Long,
    ) : Destination()

    @Serializable
    data object BookmarkedTags : Destination()

    @Serializable
    data class Following(
        val userId: Long,
    ) : Destination()

    @Serializable
    data class UserArtwork(
        val userId: Long,
    ) : Destination()

    @Serializable
    data object BlockSettings : Destination()

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
    data object DownloadBrowser : Destination()
}

@Serializable
sealed class MainPage(
    @Transient
    val icon: @Composable (() -> Unit) = {},
) {
    @Serializable
    data object Home : MainPage(
        icon = {
            Icon(
                imageVector = Icons.Rounded.Home,
                contentDescription = null,
            )
        }
    )

    @Serializable
    data object Ranking : MainPage(
        icon = {
            Icon(
                imageVector = Icons.Rounded.Equalizer,
                contentDescription = null,
            )
        }
    )

    @Serializable
    data object Latest : MainPage(
        icon = {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
            )
        }
    )

    @Serializable
    data object Search : MainPage(
        icon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
            )
        }
    )

    @Serializable
    data object Profile : MainPage(
        icon = {
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = null,
            )
        }
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
