package com.mrl.pixiv

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.VersionManager
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.MainPage
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.home.HomeScreen
import com.mrl.pixiv.latest.LatestScreen
import com.mrl.pixiv.profile.ProfileScreen
import com.mrl.pixiv.ranking.RankingScreen
import com.mrl.pixiv.search.preview.SearchPreviewScreen

@Composable
fun MainNavigationScaffold(
    showNavigation: Boolean,
    navigationManager: NavigationManager,
    content: @Composable () -> Unit,
) {
    val page = navigationManager.currentMainPage
    val hasNewVersion by VersionManager.hasNewVersion.collectAsStateWithLifecycle()
    val useLiquidGlassNavigation by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle {
        useLiquidGlassNavigation
    }
    MainNavigationLayout(
        selectedPage = page,
        hasNewVersion = hasNewVersion,
        useLiquidGlassNavigation = useLiquidGlassNavigation,
        onSelectPage = { screen ->
            if (page != screen) navigationManager.switchMainPage(screen)
        },
        layoutType = mainNavigationSuiteType(
            adaptiveType = NavigationSuiteScaffoldDefaults.navigationSuiteType(
                currentWindowAdaptiveInfoV2()
            ),
            showNavigation = showNavigation,
            currentDestination = navigationManager.currentDestination,
        ),
        content = content,
    )
}

// HotSwan 2.2.0 无法为 NavigationSuiteType 生成返回值桥接，inline 可跳过该函数的插桩。
@Suppress("NOTHING_TO_INLINE")
internal inline fun mainNavigationSuiteType(
    adaptiveType: NavigationSuiteType,
    showNavigation: Boolean,
    currentDestination: Destination,
): NavigationSuiteType {
    if (!showNavigation) return NavigationSuiteType.None
    // 大屏侧边导航属于应用框架，底部导航仅在主页面显示。
    return if (adaptiveType.isBottomNavigation && currentDestination != Destination.Main) {
        NavigationSuiteType.None
    } else {
        adaptiveType
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val navigationManager = currentNavigationManager()
    val page = navigationManager.currentMainPage
    MainPageContent(
        selectedPage = page,
        usePager = NavigationSuiteScaffoldDefaults.navigationSuiteType(
            currentWindowAdaptiveInfoV2()
        ).isBottomNavigation,
        modifier = modifier,
    ) {
        when (it) {
            MainPage.Home -> HomeScreen()
            MainPage.Ranking -> RankingScreen()
            MainPage.Latest -> LatestScreen()
            MainPage.Search -> SearchPreviewScreen()
            MainPage.Profile -> ProfileScreen()
        }
    }
}
