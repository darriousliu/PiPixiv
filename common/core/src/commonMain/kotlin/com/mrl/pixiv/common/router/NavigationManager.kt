package com.mrl.pixiv.common.router

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.repository.IllustCacheRepo
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

typealias NavigateToHorizontalPictureScreen = (
    illusts: List<Illust>,
    index: Int,
    cover: PictureCoverSource?,
) -> Unit

@Single
@Stable
class NavigationManager(
    vararg initialBackStack: Destination
) {
    private var store = NavigationStore(initialBackStack.toList())
    private var sourceEntryId: String? = null

    val backStack: List<NavigationRecord>
        get() = store.records

    val currentDestination: Destination
        get() = backStack.last().destination

    val topEntryId: String?
        get() = backStack.lastOrNull()?.entryId

    /** Identity of the entry that owns this scoped manager. */
    val entryId: String?
        get() = sourceEntryId

    val currentMainPage: MainPage
        get() = store.currentMainPage

    /** The scoped manager shares the root store, but never guesses which visible page was clicked. */
    fun forEntry(entryId: String): NavigationManager = NavigationManager().also {
        it.store = store
        it.sourceEntryId = entryId
    }

    fun saveState(): NavigationStateSnapshot = NavigationStateSnapshot(backStack, currentMainPage)

    /**
     * Restores identities into the shared store, including managers already scoped to those IDs.
     * Picture arguments still need the image cache; this is not durable image-data restoration.
     */
    fun restoreState(snapshot: NavigationStateSnapshot) {
        require(snapshot.records.isNotEmpty()) { "Navigation state must contain a root entry" }
        val seenIds = mutableSetOf<String>()
        var source: NavigationRecord? = null
        snapshot.records.forEach { record ->
            require(record.entryId.isNotBlank() && seenIds.add(record.entryId)) {
                "Navigation entry IDs must be non-empty and unique"
            }
            if (record.ownerEntryId == null) {
                source = record
            } else {
                require(
                    record.ownerEntryId == source?.entryId &&
                            source.destination.paneSpec.canHostDetail &&
                            record.destination.paneSpec.canShowAsDetail
                ) {
                    "A detail must belong to its preceding source without crossing a full-width entry"
                }
            }
        }
        store.records = snapshot.records.toList()
        store.currentMainPage = snapshot.currentMainPage
    }

    fun popBackStack() {
        val records = backStack
        if (records.size <= 1) return
        val source = activeSource(records) ?: return
        val sourceIndex = records.indexOf(source)
        // A source's own back button leaves the source and its entire detail branch.
        // The root manager and the current detail's back button pop one visit at a time.
        val keepCount =
            if (sourceEntryId != null && source.entryId == records.last().ownerEntryId) {
                sourceIndex.coerceAtLeast(1)
            } else {
                records.lastIndex
            }
        store.records = records.take(keepCount)
    }

    fun navigate(destination: Destination) {
        pushDestination { destination }
    }

    private inline fun pushDestination(createDestination: (entryId: String) -> Destination) {
        val records = backStack
        val source = activeSource(records)
        if (sourceEntryId != null && source == null) return
        val entryId = Uuid.random().toHexString()
        val destination = createDestination(entryId)
        val spec = destination.paneSpec
        val ownerEntryId = when {
            spec.preferredFullWidth -> null
            source?.ownerEntryId != null && spec.canShowAsDetail -> source.ownerEntryId
            source != null && source.destination.paneSpec.canHostDetail &&
                    spec.canShowAsDetail && !spec.preferAsSource -> source.entryId

            else -> null
        }
        // Opening Picture (or a preview) preserves the complete previous split context.
        // Selecting from the source otherwise replaces its old detail branch.
        val history = if (!spec.preferredFullWidth && source?.ownerEntryId == null &&
            source != null && records.lastOrNull()?.ownerEntryId == source.entryId
        ) {
            records.take(records.indexOf(source) + 1)
        } else {
            records
        }
        store.records = history + NavigationRecord(entryId, destination, ownerEntryId)
    }

    /** Explicitly opens a new independent visit, for example full-width novel reading. */
    fun openInMainPane() {
        val source = activeSource(backStack) ?: return
        store.records = backStack + newRecord(source.destination)
    }

    fun closeDetailBranch(ownerEntryId: String) {
        val records = backStack
        if (records.lastOrNull()?.ownerEntryId != ownerEntryId) return
        val ownerIndex = records.indexOfFirst { it.entryId == ownerEntryId }
        if (ownerIndex >= 0) store.records = records.take(ownerIndex + 1)
    }

    /** Used when a source changes its selected content without creating a navigation visit. */
    fun closeCurrentDetailBranch() {
        val source = activeSource(backStack) ?: return
        closeDetailBranch(source.ownerEntryId ?: source.entryId)
    }

    fun switchMainPage(page: MainPage) {
        if (currentMainPage != page) {
            popBackToMainScreen()
            store.currentMainPage = page
        }
    }

    fun loginToMainScreen(showDataRecovery: Boolean = false) {
        store.currentMainPage = MainPage.Home
        val home = newRecord(Destination.Main)
        store.records = if (showDataRecovery) {
            listOf(home, newRecord(Destination.DataRecovery(afterLogin = true)))
        } else {
            listOf(home)
        }
    }

    fun popBackToMainScreen() {
        val records = backStack
        val mainIndex = records.indexOfLast { it.destination == Destination.Main }
        if (mainIndex >= 0) store.records = records.take(mainIndex + 1)
    }

    private fun activeSource(records: List<NavigationRecord>): NavigationRecord? {
        val top = records.lastOrNull() ?: return null
        val entryId = sourceEntryId ?: return top
        // Ignore callbacks from disposed or outgoing entries instead of retargeting the top page.
        if (entryId != top.entryId && entryId != top.ownerEntryId) return null
        return records.firstOrNull { it.entryId == entryId }
    }

    fun navigateToPictureScreen(
        illusts: List<Illust>,
        index: Int,
        cover: PictureCoverSource? = null,
    ) {
        pushDestination { entryId ->
            IllustCacheRepo[entryId] = illusts.toList()
            Destination.Picture(
                index = index,
                cacheKey = entryId,
                cover = cover,
            )
        }
    }

    fun navigateToSinglePictureScreen(illustId: Long) {
        navigate(Destination.PictureDeeplink(illustId))
    }

    fun navigateToImagePreviewScreen(
        imageUrls: List<String>,
        initialIndex: Int,
        sharedImage: PictureImageKey? = null,
    ) {
        if (imageUrls.isEmpty()) return
        navigate(
            Destination.ImagePreview(
                imageUrls = imageUrls,
                initialIndex = initialIndex.coerceIn(0, imageUrls.lastIndex),
                sharedImage = sharedImage,
            )
        )
    }

    fun navigateToSearchResultScreen(
        searchWord: String,
        isIdSearch: Boolean = false,
        searchMode: AppViewMode = AppViewMode.ILLUST
    ) {
        navigate(destination = Destination.SearchResults(searchWord, isIdSearch, searchMode))
    }

    fun navigateToProfileDetailScreen(userId: Long) {
        navigate(destination = Destination.ProfileDetail(userId))
    }

    fun navigateToFollowingScreen(userId: Long) {
        navigate(destination = Destination.Following(userId))
    }

    fun navigateToCollectionScreen(userId: Long, isNovel: Boolean = false) {
        navigate(destination = Destination.Collection(userId, isNovel))
    }

    fun navigateToBookmarkedTagsScreen() {
        navigate(destination = Destination.BookmarkedTags)
    }

    fun navigateToNovelMarkersScreen() {
        navigate(destination = Destination.NovelMarkers)
    }

    fun navigateToSearchScreen() {
        if (backStack.lastOrNull()?.destination != Destination.Search) {
            navigate(Destination.Search)
        }
    }

    fun navigateToHistoryScreen() {
        navigate(destination = Destination.History)
    }

    fun navigateToReadingScreen() {
        navigate(destination = Destination.Reading)
    }

    fun navigateToNovelReadLaterScreen() {
        navigate(destination = Destination.NovelReadLater)
    }

    fun navigateToSettingScreen() {
        navigate(destination = Destination.Setting)
    }

    fun navigateToAccountManagementScreen() {
        navigate(Destination.AccountManagement)
    }

    fun navigateToLoginOptionScreen() {
        store.records = listOf(newRecord(Destination.LoginOption))
    }

    fun navigateToAddAccountScreen() {
        navigate(Destination.AddAccount)
    }

    fun navigateToUserIllustScreen(userId: Long, initialType: Type = Type.Illust) {
        navigate(destination = Destination.UserArtwork(userId, initialType))
    }

    fun navigateToUserNovelsScreen(userId: Long) {
        navigate(destination = Destination.UserNovels(userId))
    }

    fun navigateToBlockSettings() {
        navigate(destination = Destination.BlockSettings)
    }

    fun navigateToAppDataScreen() {
        navigate(destination = Destination.AppData)
    }

    fun navigateToNetworkSettingScreen() {
        navigate(destination = Destination.NetworkSetting)
    }

    fun navigateToNetworkDiagnosticScreen() {
        navigate(destination = Destination.NetworkDiagnostic)
    }

    fun navigateToAppearanceSettingScreen() {
        navigate(destination = Destination.AppearanceSetting)
    }

    fun navigateToBrowsingSettingScreen() {
        navigate(destination = Destination.BrowsingSetting)
    }

    fun navigateToSearchSettingScreen() {
        navigate(destination = Destination.SearchSetting)
    }

    fun navigateToHistorySettingScreen() {
        navigate(destination = Destination.HistorySetting)
    }

    fun navigateToPrivacySettingScreen() {
        navigate(destination = Destination.PrivacySetting)
    }

    fun navigateToFileNameFormatScreen() {
        navigate(destination = Destination.FileNameFormat)
    }

    fun navigateToAiTranslationSettingScreen() {
        navigate(destination = Destination.AiTranslationSetting)
    }

    fun navigateToDownloadScreen() {
        navigate(destination = Destination.Download)
    }

    fun navigateToAboutScreen() {
        navigate(destination = Destination.About)
    }

    fun navigateToAppLogScreen() {
        navigate(destination = Destination.AppLog)
    }

    fun navigateToCommentScreen(id: Long, type: CommentType) {
        navigate(destination = Destination.Comment(id, type))
    }

    fun navigateToReportCommentScreen(commentId: Long, type: ReportType) {
        navigate(destination = Destination.Report(commentId, type))
    }

    fun navigateToNovelDetailScreen(
        novelId: Long,
        markerPage: Int? = null,
        readLaterTargetLanguage: String? = null,
    ) {
        navigate(
            destination = Destination.NovelDetail(
                novelId = novelId,
                markerPage = markerPage,
                readLaterTargetLanguage = readLaterTargetLanguage,
            )
        )
    }

    fun navigateToNovelSeriesScreen(seriesId: Long) {
        navigate(destination = Destination.NovelSeries(seriesId))
    }

    fun navigateToMangaSeriesScreen(seriesId: Long) {
        navigate(Destination.MangaSeries(seriesId))
    }

    fun navigateToMangaWatchlistScreen() {
        navigate(Destination.MangaWatchlist)
    }

    fun navigateToUserMangaSeriesScreen(userId: Long) {
        navigate(Destination.UserMangaSeries(userId))
    }

    fun navigateToNotifications(notificationId: Long? = null, title: String? = null) {
        navigate(Destination.Notifications(notificationId, title))
    }

    fun navigateToRecommendedUsers() {
        navigate(Destination.CommunityUsers(com.mrl.pixiv.common.data.discovery.CommunityUsersKind.RECOMMENDED))
    }

    fun navigateToRelatedUsers(userId: Long) {
        navigate(
            Destination.CommunityUsers(
                com.mrl.pixiv.common.data.discovery.CommunityUsersKind.RELATED,
                userId
            )
        )
    }

    fun navigateToFollowers() {
        navigate(Destination.CommunityUsers(com.mrl.pixiv.common.data.discovery.CommunityUsersKind.FOLLOWERS))
    }
}

private class NavigationStore(initialDestinations: List<Destination>) {
    var records by mutableStateOf(initialDestinations.map { newRecord(it) })
    var currentMainPage by mutableStateOf<MainPage>(MainPage.Home)
}

private fun newRecord(destination: Destination) = NavigationRecord(
    entryId = Uuid.random().toHexString(),
    destination = destination,
)
