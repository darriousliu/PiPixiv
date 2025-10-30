package com.mrl.pixiv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mrl.pixiv.common.router.MainPage
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
    val screens = remember {
        listOf(
            MainPage.HOME,
            MainPage.LATEST,
            MainPage.SEARCH,
            MainPage.PROFILE,
        )
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            screens.forEach { screen ->
                item(
                    selected = page == screen,
                    onClick = {
                        if (page != screen) {
                            navigationManager.switchMainPage(screen)
                        }
                    },
                    icon = screen.icon,
                    label = {
                        Text(text = stringResource(screen.title))
                    }
                )
            }
        },
        layoutType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
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
                MainPage.HOME -> HomeScreen()
                MainPage.LATEST -> LatestScreen()
                MainPage.SEARCH -> SearchPreviewScreen()
                MainPage.PROFILE -> ProfileScreen()
            }
        }
    }
}