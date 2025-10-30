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
    ) : Destination()

    @Serializable
    data object Setting : Destination()

    @Serializable
    data object NetworkSetting : Destination()

    @Serializable
    data object History : Destination()

    @Serializable
    data class Collection(
        val userId: Long,
    ) : Destination()

    @Serializable
    data class Following(
        val userId: Long,
    ) : Destination()
}

@Serializable
enum class MainPage(
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