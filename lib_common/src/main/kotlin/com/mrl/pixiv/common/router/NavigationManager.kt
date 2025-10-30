package com.mrl.pixiv.common.router

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
class NavigationManager(
    vararg initialBackStack: Destination
) {
    var currentMainPage by mutableStateOf(MainPage.HOME)
        private set

    val backStack = mutableStateListOf(*initialBackStack)

    val currentDestination: Destination
        get() = backStack.last()

    fun switchMainPage(page: MainPage) {
        if (currentMainPage != page) {
            currentMainPage = page
        }
    }

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
        backStack.add(Destination.Main)
    }

    fun popBackToMainScreen() {
        popBackStack(route = Destination.Main, inclusive = false)
    }

    fun navigateToPictureScreen(
        illusts: List<Illust>,
        index: Int,
        prefix: String,
        enableTransition: Boolean,
    ) {
        measureTime {
            IllustCacheRepo[prefix] = illusts
            navigate(Destination.Picture(index, prefix, enableTransition))
        }.let {
            Logger.i("Navigation") { "navigateToPictureScreen cost: $it" }
        }
    }

    fun navigateToSearchResultScreen(searchWord: String) {
        navigate(route = Destination.SearchResults(searchWord))
    }

    fun navigateToProfileDetailScreen(userId: Long) {
        navigate(route = Destination.ProfileDetail(userId))
    }

    fun navigateToFollowingScreen(userId: Long) {
        navigate(route = Destination.Following(userId))
    }

    fun navigateToCollectionScreen(userId: Long) {
        navigate(route = Destination.Collection(userId))
    }

    fun navigateToSearchScreen() {
        addSingleTop(route = Destination.Search)
    }

    fun navigateToHistoryScreen() {
        navigate(route = Destination.History)
    }

    fun navigateToSettingScreen() {
        navigate(route = Destination.Setting)
    }

    fun navigateToLoginOptionScreen() {
        backStack.clear()
        addSingleTop(route = Destination.LoginOption)
    }
}
