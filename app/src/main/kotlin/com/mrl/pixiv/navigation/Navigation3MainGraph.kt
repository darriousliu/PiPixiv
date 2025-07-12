package com.mrl.pixiv.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.mrl.pixiv.collection.CollectionScreen
import com.mrl.pixiv.common.compose.LocalSharedKeyPrefix
import com.mrl.pixiv.common.compose.LocalSharedTransitionScope
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastExpanded
import com.mrl.pixiv.common.compose.ui.bar.HomeBottomBar
import com.mrl.pixiv.common.repository.IllustCacheRepo
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.DestinationsDeepLink
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.follow.FollowingScreen
import com.mrl.pixiv.history.HistoryScreen
import com.mrl.pixiv.home.HomeScreen
import com.mrl.pixiv.latest.LatestScreen
import com.mrl.pixiv.login.LoginOptionScreen
import com.mrl.pixiv.login.LoginScreen
import com.mrl.pixiv.login.oauth.OAuthLoginScreen
import com.mrl.pixiv.picture.HorizontalSwipePictureScreen
import com.mrl.pixiv.picture.PictureDeeplinkScreen
import com.mrl.pixiv.profile.ProfileScreen
import com.mrl.pixiv.profile.detail.ProfileDetailScreen
import com.mrl.pixiv.search.SearchScreen
import com.mrl.pixiv.search.preview.SearchPreviewScreen
import com.mrl.pixiv.search.result.SearchResultsScreen
import com.mrl.pixiv.setting.SettingScreen
import com.mrl.pixiv.setting.SettingViewModel
import com.mrl.pixiv.setting.network.NetworkSettingScreen
import com.mrl.pixiv.splash.SplashViewModel
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf


@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun Navigation3MainGraph(
    startDestination: Destination,
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject { parametersOf(arrayOf(startDestination)) }
) {
    val settingViewModel: SettingViewModel =
        koinViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity)
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(windowAdaptiveInfo)
    val currentRoute = navigationManager.currentDestination
    val bottomBarVisibility = bottomBarVisibility(currentRoute, windowAdaptiveInfo)
    val listDetailStrategy = rememberListDetailSceneStrategy<Any>()

    HandleDeeplink(navigationManager)
    SharedTransitionLayout {
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this
        ) {
            NavigationSuiteScaffoldLayout(
                navigationSuite = {
                    HomeBottomBar(
                        bottomBarVisibility = bottomBarVisibility,
                        layoutType = layoutType,
                        currentRoute = currentRoute,
                        onSwitch = {
                            navigationManager.addSingleTop(it)
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
                    NavDisplay(
                        backStack = navigationManager.backStack,
                        modifier = modifier,
                        entryDecorators = listOf(
                            // Add the default decorators for managing scenes and saving state
                            rememberSceneSetupNavEntryDecorator(),
                            rememberSavedStateNavEntryDecorator(),
                            // Then add the view model store decorator
                            rememberViewModelStoreNavEntryDecorator()
                        ),
                        sceneStrategy = listDetailStrategy,
                        entryProvider = entryProvider {
                            entry<Destination.LoginOptionScreen> {
                                LoginOptionScreen()
                            }
                            // 登陆
                            entry<Destination.LoginScreen> {
                                val startUrl = it.startUrl
                                LoginScreen(startUrl = startUrl)
                            }
                            // OAuth token登陆
                            entry<Destination.OAuthLoginScreen> {
                                OAuthLoginScreen()
                            }

                            // 首页
                            entry<Destination.HomeScreen>(
                                metadata = ListDetailSceneStrategy.listPane()
                            ) {
                                HomeScreen()
                            }

                            // 新作页面
                            entry<Destination.LatestScreen>(
                                metadata = ListDetailSceneStrategy.listPane()
                            ) {
                                LatestScreen()
                            }

                            // 搜索预览页
                            entry<Destination.SearchPreviewScreen>(
                                metadata = ListDetailSceneStrategy.listPane()
                            ) {
                                SearchPreviewScreen()
                            }

                            // 个人主页
                            entry<Destination.ProfileScreen>(
                                metadata = ListDetailSceneStrategy.listPane()
                            ) {
                                ProfileScreen()
                            }

                            // 详情页
                            entry<Destination.ProfileDetailScreen>(
                                metadata = ListDetailSceneStrategy.detailPane() +
                                        ListDetailSceneStrategy.extraPane()
                            ) {
                                ProfileDetailScreen(
                                    uid = it.userId
                                )
                            }

                            // 作品详情页（深度链接）
                            entry<Destination.PictureDeeplinkScreen>(
                                metadata = ListDetailSceneStrategy.detailPane() +
                                        NavDisplay.transitionSpec {
                                            scaleIn(initialScale = 0.9f) + fadeIn() togetherWith
                                                    scaleOut(targetScale = 1.1f) + fadeOut()
                                        } + NavDisplay.predictivePopTransitionSpec {
                                    scaleIn(initialScale = 1.1f) + fadeIn() togetherWith
                                            scaleOut(targetScale = 0.9f) + fadeOut()
                                },
                            ) {
                                val illustId = it.illustId
                                PictureDeeplinkScreen(
                                    illustId = illustId,
                                )
                            }

                            // 搜索页
                            entry<Destination.SearchScreen>(
                                metadata = ListDetailSceneStrategy.detailPane() +
                                        ListDetailSceneStrategy.extraPane()
                            ) {
                                SearchScreen()
                            }

                            // 搜索结果页
                            entry<Destination.SearchResultsScreen>(
                                metadata = ListDetailSceneStrategy.detailPane() +
                                        ListDetailSceneStrategy.extraPane()
                            ) {
                                val searchWord = it.searchWords
                                SearchResultsScreen(
                                    searchWords = searchWord,
                                )
                            }

                            // 设置页
                            entry<Destination.SettingScreen>(
                                metadata = ListDetailSceneStrategy.detailPane()
                            ) {
                                SettingScreen()
                            }

                            // 网络设置页
                            entry<Destination.NetworkSettingScreen>(
                                metadata = ListDetailSceneStrategy.detailPane() +
                                        ListDetailSceneStrategy.extraPane()
                            ) {
                                NetworkSettingScreen(viewModel = settingViewModel)
                            }

                            // 历史记录
                            entry<Destination.HistoryScreen>(
                                metadata = ListDetailSceneStrategy.detailPane()
                            ) {
                                HistoryScreen()
                            }

                            // 本人收藏页
                            entry<Destination.CollectionScreen>(
                                metadata = ListDetailSceneStrategy.detailPane()
                            ) {
                                val userId = it.userId
                                CollectionScreen(uid = userId)
                            }

                            entry<Destination.FollowingScreen>(
                                metadata = ListDetailSceneStrategy.detailPane()
                            ) {
                                val uid = it.userId
                                FollowingScreen(uid = uid)
                            }

                            // 横向滑动作品详情页
                            entry<Destination.PictureScreen>(
                                metadata = ListDetailSceneStrategy.detailPane() +
                                        NavDisplay.transitionSpec {
                                            scaleIn(initialScale = 0.9f) + fadeIn() togetherWith
                                                    scaleOut(targetScale = 1.1f) + fadeOut()
                                        } + NavDisplay.predictivePopTransitionSpec {
                                    scaleIn(initialScale = 1.1f) + fadeIn() togetherWith
                                            scaleOut(targetScale = 0.9f) + fadeOut()
                                }
                            ) {
                                val params = it
                                val illusts = remember { IllustCacheRepo[params.prefix] }
                                CompositionLocalProvider(
                                    LocalSharedKeyPrefix provides params.prefix
                                ) {
                                    HorizontalSwipePictureScreen(
                                        illusts = illusts.toImmutableList(),
                                        index = params.index,
                                        prefix = params.prefix,
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HandleDeeplink(
    navigationManager: NavigationManager,
) {
    val splashViewModel: SplashViewModel =
        koinViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity)
    val intent = splashViewModel.intent.collectAsStateWithLifecycle().value
    LaunchedEffect(intent) {
        if (intent != null) {
            val data = intent.data ?: return@LaunchedEffect
            when {
                DestinationsDeepLink.illustRegex.matches(data.toString()) -> {
                    navigationManager.navigate(
                        Destination.PictureDeeplinkScreen(
                            data.lastPathSegment?.toLong() ?: 0
                        )
                    )
                }

                DestinationsDeepLink.userRegex.matches(data.toString()) -> {
                    navigationManager.navigate(
                        Destination.ProfileDetailScreen(
                            data.lastPathSegment?.toLong() ?: 0
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun bottomBarVisibility(
    currentRoute: Destination,
    windowAdaptiveInfo: WindowAdaptiveInfo,
): Boolean {
    return currentRoute in remember {
        listOf(
            Destination.HomeScreen,
            Destination.LatestScreen,
            Destination.SearchPreviewScreen,
            Destination.ProfileScreen
        )
    } || windowAdaptiveInfo.isWidthAtLeastExpanded
}
