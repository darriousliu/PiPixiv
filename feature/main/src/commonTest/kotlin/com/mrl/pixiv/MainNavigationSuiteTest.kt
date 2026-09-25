package com.mrl.pixiv

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.MainPage
import com.mrl.pixiv.common.router.NavigationManager
import kotlin.test.Test
import kotlin.test.assertEquals

class MainNavigationSuiteTest {
    private val bottomBars = listOf(
        NavigationSuiteType.NavigationBar,
        NavigationSuiteType.ShortNavigationBarCompact,
        NavigationSuiteType.ShortNavigationBarMedium,
    )

    @Test
    fun bottomTabsFollowMainWhenOpeningAndReturningFromSecondaryPages() {
        val destinations = listOf(
            Destination.Setting,
            Destination.Search,
            Destination.SearchResults("landscape", false),
            Destination.History,
            Destination.Collection(1, isNovel = false),
            Destination.ProfileDetail(1),
            Destination.NovelDetail(1),
        )
        for (type in bottomBars) {
            for (destination in destinations) {
                val navigation = NavigationManager(Destination.Main)
                assertEquals(type, typeFor(navigation, type))

                navigation.navigate(destination)
                assertEquals(NavigationSuiteType.None, typeFor(navigation, type), "$type: $destination")

                navigation.popBackStack()
                assertEquals(type, typeFor(navigation, type))
            }
        }
    }

    @Test
    fun nestedSettingsKeepBottomTabsHiddenUntilReturningToMain() {
        val navigation = NavigationManager(Destination.Main)
        val type = NavigationSuiteType.ShortNavigationBarCompact

        navigation.navigate(Destination.Setting)
        navigation.navigate(Destination.NetworkSetting)
        assertEquals(NavigationSuiteType.None, typeFor(navigation, type))
        navigation.popBackStack()
        assertEquals(NavigationSuiteType.None, typeFor(navigation, type))
        navigation.popBackStack()
        assertEquals(type, typeFor(navigation, type))
    }

    @Test
    fun everyMainTabRetainsBottomNavigation() {
        val navigation = NavigationManager(Destination.Main)
        for (page in listOf(MainPage.Home, MainPage.Ranking, MainPage.Latest, MainPage.Search, MainPage.Profile)) {
            navigation.switchMainPage(page)
            for (type in bottomBars) {
                assertEquals(type, typeFor(navigation, type))
            }
        }
    }

    @Test
    fun resizingASecondaryPageHidesBottomTabsAndRestoresTheRail() {
        val navigation = NavigationManager(Destination.Main)
        navigation.navigate(Destination.Setting)
        val rails = listOf(
            NavigationSuiteType.NavigationRail,
            NavigationSuiteType.NavigationDrawer,
            NavigationSuiteType.WideNavigationRailCollapsed,
            NavigationSuiteType.WideNavigationRailExpanded,
        )
        for (rail in rails) {
            assertEquals(rail, typeFor(navigation, rail))
            for (bar in bottomBars) {
                assertEquals(NavigationSuiteType.None, typeFor(navigation, bar))
            }
            assertEquals(rail, typeFor(navigation, rail))
            assertEquals(
                NavigationSuiteType.None,
                mainNavigationSuiteType(rail, showNavigation = false, navigation.currentDestination),
            )
        }
    }

    @Test
    fun fullScreenAndLoginDestinationsCannotShowBottomTabs() {
        for (destination in listOf(Destination.LoginOption, Destination.PictureDeeplink(1))) {
            for (type in bottomBars) {
                assertEquals(
                    NavigationSuiteType.None,
                    mainNavigationSuiteType(type, showNavigation = false, destination),
                )
            }
        }
    }

    private fun typeFor(navigation: NavigationManager, type: NavigationSuiteType) =
        mainNavigationSuiteType(type, showNavigation = true, navigation.currentDestination)
}
