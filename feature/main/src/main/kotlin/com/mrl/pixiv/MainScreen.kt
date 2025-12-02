package com.mrl.pixiv

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.mrl.pixiv.common.router.MainPage
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.logEvent
import com.mrl.pixiv.home.HomeScreen
import com.mrl.pixiv.latest.LatestScreen
import com.mrl.pixiv.profile.ProfileScreen
import com.mrl.pixiv.ranking.RankingScreen
import com.mrl.pixiv.search.preview.SearchPreviewScreen
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class, InternalSerializationApi::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val navigationManager = koinInject<NavigationManager>()
    val page = navigationManager.currentMainPage
    val screens = remember {
        listOf(
            MainPage.Home,
            MainPage.Ranking,
            MainPage.Latest,
            MainPage.Search,
            MainPage.Profile,
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
        NavDisplay(
            backStack = navigationManager.mainBackStack,
            modifier = modifier.fillMaxSize(),
            entryProvider = entryProvider {
                entry<MainPage.Home> {
                    HomeScreen()
                }
                entry<MainPage.Ranking> {
                    RankingScreen()
                }
                entry<MainPage.Latest> {
                    LatestScreen()
                }
                entry<MainPage.Search> {
                    SearchPreviewScreen()
                }
                entry<MainPage.Profile> {
                    ProfileScreen()
                }
            }
        )
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