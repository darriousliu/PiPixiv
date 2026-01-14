package com.mrl.pixiv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.analytics.logEvent
import com.mrl.pixiv.common.repository.VersionManager
import com.mrl.pixiv.common.router.MainPage
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.home.HomeScreen
import com.mrl.pixiv.latest.LatestScreen
import com.mrl.pixiv.profile.ProfileScreen
import com.mrl.pixiv.ranking.RankingScreen
import com.mrl.pixiv.search.preview.SearchPreviewScreen
import com.mrl.pixiv.strings.home
import com.mrl.pixiv.strings.my
import com.mrl.pixiv.strings.new_artworks
import com.mrl.pixiv.strings.ranking
import com.mrl.pixiv.strings.search
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class, InternalSerializationApi::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val navigationManager = koinInject<NavigationManager>()
    val page = navigationManager.currentMainPage
    val hasNewVersion by VersionManager.hasNewVersion.collectAsStateWithLifecycle()
    val screens = remember {
        listOf(
            MainPage.Home to RStrings.home,
            MainPage.Ranking to RStrings.ranking,
            MainPage.Latest to RStrings.new_artworks,
            MainPage.Search to RStrings.search,
            MainPage.Profile to RStrings.my,
        )
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            screens.forEach { (screen, title) ->
                item(
                    selected = page == screen,
                    onClick = {
                        if (page != screen) {
                            navigationManager.switchMainPage(screen)
                        }
                    },
                    icon = {
                        if (screen == MainPage.Profile && hasNewVersion) {
                            BadgedBox(
                                badge = { Badge() }
                            ) {
                                screen.icon()
                            }
                        } else {
                            screen.icon()
                        }
                    },
                    label = {
                        Text(text = stringResource(title))
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
                MainPage.Home -> HomeScreen()
                MainPage.Ranking -> RankingScreen()
                MainPage.Latest -> LatestScreen()
                MainPage.Search -> SearchPreviewScreen()
                MainPage.Profile -> ProfileScreen()
            }
        }
    }
    LaunchedEffect(navigationManager.currentMainPage) {
        logEvent("screen_view", buildMap {
            val screenName =
                navigationManager.currentMainPage::class.serializer().descriptor.serialName
                    .split(".")
                    .lastOrNull()
                    .orEmpty()
            put("screen_name", screenName)
            put("screen_class", screenName)
        })
    }
}
