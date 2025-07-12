package com.mrl.pixiv.common.router

import androidx.compose.runtime.mutableStateListOf
import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.repository.IllustCacheRepo
import org.koin.core.annotation.Single
import kotlin.time.measureTime

typealias NavigateToHorizontalPictureScreen = (illusts: List<Illust>, index: Int, prefix: String) -> Unit

@Single
class NavigationManager(
    vararg initialBackStack: Destination
) {
    val backStack = mutableStateListOf(*initialBackStack)

    val currentDestination: Destination
        get() = backStack.last()

    fun addSingleTop(route: Destination): Boolean {
        val currentIndex = backStack.indexOfFirst { it == route }
        return if (currentIndex != -1) {
            backStack.add(backStack.removeAt(currentIndex))
        } else {
            backStack.add(route)
        }
    }

    fun navigate(route: Destination): Boolean {
        return backStack.add(route)
    }

    fun popBackStack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun popBackStack(route: Destination, inclusive: Boolean) {
        if (backStack.size > 1) {
            val index = backStack.indexOfLast { it == route }
            if (index != -1) {
                if (inclusive) {
                    backStack.removeRange(index, backStack.size)
                } else {
                    backStack.removeRange(index + 1, backStack.size)
                }
            }
        }
    }

    fun loginToMainScreen() {
        backStack.clear()
        backStack.add(Destination.HomeScreen)
    }

    fun popBackToMainScreen() {
        popBackStack(route = Destination.HomeScreen, inclusive = false)
    }

    fun navigateToPictureScreen(
        illusts: List<Illust>,
        index: Int,
        prefix: String
    ) {
        measureTime {
            IllustCacheRepo[prefix] = illusts
            navigate(Destination.PictureScreen(index, prefix))
        }.let {
            Logger.i("Navigation") { "navigateToPictureScreen cost: $it" }
        }
    }

    fun navigateToSearchResultScreen(searchWord: String) {
        navigate(route = Destination.SearchResultsScreen(searchWord))
    }

    fun navigateToProfileDetailScreen(userId: Long) {
        navigate(route = Destination.ProfileDetailScreen(userId))
    }

    fun navigateToFollowingScreen(userId: Long) {
        navigate(route = Destination.FollowingScreen(userId))
    }

    fun navigateToCollectionScreen(userId: Long) {
        navigate(route = Destination.CollectionScreen(userId))
    }

    fun navigateToSearchScreen() {
        addSingleTop(route = Destination.SearchScreen)
    }

    fun navigateToHistoryScreen() {
        navigate(route = Destination.HistoryScreen)
    }

    fun navigateToSettingScreen() {
        navigate(route = Destination.SettingScreen)
    }

    fun navigateToLoginOptionScreen() {
        backStack.clear()
        addSingleTop(route = Destination.LoginOptionScreen)
    }
}
