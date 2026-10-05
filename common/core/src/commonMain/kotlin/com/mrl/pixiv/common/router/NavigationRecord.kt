package com.mrl.pixiv.common.router

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A visit has its own identity, even when another visit has exactly the same arguments. */
@Immutable
@Serializable
data class NavigationRecord(
    val entryId: String,
    val destination: Destination,
    val ownerEntryId: String? = null,
) : NavKey

@Serializable
data class NavigationStateSnapshot(
    val records: List<NavigationRecord>,
    val currentMainPage: MainPage,
)

/** Only navigation arguments are saved; Picture's in-memory image cache is not persisted here. */
val NavigationStateSnapshotSaver = Saver<NavigationStateSnapshot, String>(
    save = { Json.encodeToString(NavigationStateSnapshot.serializer(), it) },
    restore = {
        runCatching {
            Json.decodeFromString(NavigationStateSnapshot.serializer(), it)
        }.getOrNull()
    },
)

@Immutable
data class DestinationPaneSpec(
    val canShowAsDetail: Boolean = true,
    val canHostDetail: Boolean = true,
    val minSourceWidth: Dp = 360.dp,
    val minDetailWidth: Dp = 420.dp,
    val preferredFullWidth: Boolean = false,
    /** An independent visit starts a source; a visit from an existing detail stays in its branch. */
    val preferAsSource: Boolean = false,
)

private val sourcePaneSpec = DestinationPaneSpec(preferAsSource = true)
private val detailPaneSpec = DestinationPaneSpec()
private val mainPaneSpec = sourcePaneSpec.copy(canShowAsDetail = false)
private val picturePaneSpec = DestinationPaneSpec(
    canShowAsDetail = false,
    minSourceWidth = 840.dp,
    preferredFullWidth = true,
)
private val exclusivePaneSpec = DestinationPaneSpec(
    canShowAsDetail = false,
    canHostDetail = false,
    preferredFullWidth = true,
)

/** Capabilities are independent from the window size; resizing never changes navigation history. */
val Destination.paneSpec: DestinationPaneSpec
    get() = when (this) {
        Destination.Main -> mainPaneSpec

        Destination.Search,
        is Destination.SearchResults,
        Destination.Setting,
        Destination.BlockSettings,
        Destination.History,
        Destination.NovelReadLater,
        is Destination.Collection,
        Destination.BookmarkedTags,
        Destination.NovelMarkers,
        is Destination.Following,
        is Destination.UserArtwork,
        is Destination.UserNovels,
        is Destination.NovelSeries,
        is Destination.MangaSeries,
        Destination.MangaWatchlist,
        is Destination.UserMangaSeries,
        is Destination.Notifications,
        is Destination.CommunityUsers,
        Destination.Download -> sourcePaneSpec

        is Destination.Picture,
        is Destination.PictureDeeplink -> picturePaneSpec

        Destination.LoginOption,
        is Destination.Login,
        Destination.OAuthLogin,
        Destination.WebCookieLogin,
        is Destination.ImagePreview -> exclusivePaneSpec

        is Destination.ProfileDetail,
        is Destination.NovelDetail,
        is Destination.Comment,
        is Destination.Report,
        Destination.NetworkSetting,
        Destination.BrowsingSetting,
        Destination.SearchSetting,
        Destination.HistorySetting,
        Destination.PrivacySetting,
        Destination.FileNameFormat,
        Destination.AiTranslationSetting,
        Destination.BlockIllust,
        Destination.BlockNovel,
        Destination.BlockUser,
        Destination.BlockTag,
        Destination.CloudMute,
        Destination.BlockComments,
        Destination.AppData,
        Destination.About -> detailPaneSpec
    }
