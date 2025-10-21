package com.mrl.pixiv.common.router

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.mrl.pixiv.common.util.RString
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class Destination : NavKey {
    @Serializable
    data object LoginOptionScreen : Destination()

    @Serializable
    data class LoginScreen(
        val startUrl: String,
    ) : Destination()

    @Serializable
    data object OAuthLoginScreen : Destination()

    @Serializable
    data object MainScreen : Destination()

    @Serializable
    data class ProfileDetailScreen(
        val userId: Long,
    ) : Destination()

    @Serializable
    data class PictureScreen(
        val index: Int,
        val prefix: String,
        val enableTransition: Boolean,
    ) : Destination()

    @Serializable
    data class PictureDeeplinkScreen(
        val illustId: Long,
    ) : Destination()

    @Serializable
    data object SearchScreen : Destination()

    @Serializable
    data class SearchResultsScreen(
        val searchWords: String,
    ) : Destination()

    @Serializable
    data object SettingScreen : Destination()

    @Serializable
    data object NetworkSettingScreen : Destination()

    @Serializable
    data object HistoryScreen : Destination()

    @Serializable
    data class CollectionScreen(
        val userId: Long,
    ) : Destination()

    @Serializable
    data class FollowingScreen(
        val userId: Long,
    ) : Destination()
}

@Serializable
enum class MainScreenPage(
    @Transient
    @StringRes
    val title: Int = 0,
    @Transient
    val icon: @Composable (() -> Unit) = {},
) {
    HOME(
        title = RString.home,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Home,
                contentDescription = null,
            )
        }
    ),
    LATEST(
        title = RString.new_artworks,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
            )
        }
    ),
    SEARCH(
        title = RString.search,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
            )
        }
    ),
    PROFILE(
        title = RString.my,
        icon = {
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = null,
            )
        }
    )
}