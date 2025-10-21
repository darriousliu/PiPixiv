package com.mrl.pixiv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mrl.pixiv.common.compose.ui.bar.HomeBottomBar
import com.mrl.pixiv.common.router.MainScreenPage
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.home.HomeScreen
import com.mrl.pixiv.latest.LatestScreen
import com.mrl.pixiv.profile.ProfileScreen
import com.mrl.pixiv.search.preview.SearchPreviewScreen
import org.koin.compose.koinInject

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val navigationManager = koinInject<NavigationManager>()
    val page = navigationManager.currentMainPage
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(windowAdaptiveInfo)
    NavigationSuiteScaffoldLayout(
        navigationSuite = {
            HomeBottomBar(
                layoutType = layoutType,
                currentPage = navigationManager.currentMainPage,
                onSwitch = { destination ->
                    navigationManager.switchMainPage(destination)
                }
            )
        },
        layoutType = layoutType
    ) {
        Box(
            Modifier.consumeWindowInsets(
                when (layoutType) {
                    NavigationSuiteType.NavigationBar ->
                        NavigationBarDefaults.windowInsets.only(WindowInsetsSides.Bottom)

                    NavigationSuiteType.NavigationRail ->
                        NavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start)

                    NavigationSuiteType.NavigationDrawer ->
                        DrawerDefaults.windowInsets.only(WindowInsetsSides.Start)

                    else -> WindowInsets(0, 0, 0, 0)
                }
            )
        ) {
            AnimatedContent(
                targetState = page,
                modifier = modifier,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220, delayMillis = 90))
                        .togetherWith(fadeOut(animationSpec = tween(90)))

                }
            ) {
                when (it) {
                    MainScreenPage.HOME -> HomeScreen()
                    MainScreenPage.LATEST -> LatestScreen()
                    MainScreenPage.SEARCH -> SearchPreviewScreen()
                    MainScreenPage.PROFILE -> ProfileScreen()
                }
            }
        }
    }

}