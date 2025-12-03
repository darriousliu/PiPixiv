package com.mrl.pixiv.common.router

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.repository.IllustCacheRepo
import org.koin.core.annotation.Single
import kotlin.time.measureTime

typealias NavigateToHorizontalPictureScreen = (
    illusts: List<Illust>,
    index: Int,
    prefix: String,
    enableTransition: Boolean
) -> Unit

@Single
@Stable
class NavigationManager(
    vararg initialBackStack: Destination
) {
    val backStack = mutableStateListOf(*initialBackStack)

    val currentDestination: NavKey
        get() = backStack.last()

    var currentMainPage by mutableStateOf<MainPage>(MainPage.Home)

    private fun <T : NavKey> SnapshotStateList<T>.addSingleTop(route: T): Boolean {
        val currentIndex = indexOfFirst { it == route }
        return if (currentIndex != -1) {
            add(removeAt(currentIndex))
        } else {
            add(route)
        }
    }

    private fun <T : NavKey> SnapshotStateList<T>.navigate(route: T): Boolean {
        return add(route)
    }

    private fun <T : NavKey> SnapshotStateList<T>.popBackStack() {
        if (size > 1) {
            removeAt(backStack.lastIndex)
        }
    }

    private fun <T : NavKey> SnapshotStateList<T>.popBackStack(route: T, inclusive: Boolean) {
        if (size > 1) {
            val index = indexOfLast { it == route }
            if (index != -1) {
                if (inclusive) {
                    removeRange(index, size)
                } else {
                    removeRange(index + 1, size)
                }
            }
        }
    }

    fun popBackStack() {
        backStack.popBackStack()
    }

    fun navigate(destination: Destination) {
        backStack.navigate(destination)
    }

    fun switchMainPage(page: MainPage) {
        if (currentMainPage != page) {
            currentMainPage = page
        }
    }

    fun loginToMainScreen() {
        backStack.clear()
        backStack.add(Destination.Main)
    }

    fun popBackToMainScreen() {
        backStack.popBackStack(route = Destination.Main, inclusive = false)
    }

    fun navigateToPictureScreen(
        illusts: List<Illust>,
        index: Int,
        prefix: String,
        enableTransition: Boolean,
    ) {
        measureTime {
            IllustCacheRepo[prefix] = illusts
            backStack.navigate(Destination.Picture(index, prefix, enableTransition))
        }.let {
            Logger.i("Navigation") { "navigateToPictureScreen cost: $it" }
        }
    }

    fun navigateToSearchResultScreen(searchWord: String) {
        backStack.navigate(route = Destination.SearchResults(searchWord))
    }

    fun navigateToProfileDetailScreen(userId: Long) {
        backStack.navigate(route = Destination.ProfileDetail(userId))
    }

    fun navigateToFollowingScreen(userId: Long) {
        backStack.navigate(route = Destination.Following(userId))
    }

    fun navigateToCollectionScreen(userId: Long) {
        backStack.navigate(route = Destination.Collection(userId))
    }

    fun navigateToBookmarkedTagsScreen() {
        backStack.navigate(route = Destination.BookmarkedTags)
    }

    fun navigateToSearchScreen() {
        backStack.addSingleTop(route = Destination.Search)
    }

    fun navigateToHistoryScreen() {
        backStack.navigate(route = Destination.History)
    }

    fun navigateToSettingScreen() {
        backStack.navigate(route = Destination.Setting)
    }

    fun navigateToLoginOptionScreen() {
        backStack.clear()
        backStack.addSingleTop(route = Destination.LoginOption)
    }

    fun navigateToUserIllustScreen(userId: Long) {
        backStack.navigate(route = Destination.UserArtwork(userId))
    }

    fun navigateToBlockSettings() {
        backStack.navigate(route = Destination.BlockSettings)
    }

    fun navigateToAppDataScreen() {
        backStack.navigate(route = Destination.AppData)
    }

    fun navigateToNetworkSettingScreen() {
        backStack.navigate(route = Destination.NetworkSetting)
    }
}
