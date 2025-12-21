package com.mrl.pixiv.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dokar.sonner.LocalToastContentColor
import com.dokar.sonner.Toaster
import com.dokar.sonner.ToasterState
import com.dokar.sonner.rememberToasterState
import com.mrl.pixiv.MainScreen
import com.mrl.pixiv.artwork.ArtworkScreen
import com.mrl.pixiv.collection.CollectionScreen
import com.mrl.pixiv.collection.tags.BookmarkedTagsScreen
import com.mrl.pixiv.comment.BlockCommentsScreen
import com.mrl.pixiv.comment.CommentScreen
import com.mrl.pixiv.common.analytics.logEvent
import com.mrl.pixiv.common.animation.DefaultFloatAnimationSpec
import com.mrl.pixiv.common.compose.LocalSharedKeyPrefix
import com.mrl.pixiv.common.compose.LocalSharedTransitionScope
import com.mrl.pixiv.common.compose.LocalToaster
import com.mrl.pixiv.common.repository.IllustCacheRepo
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.DestinationsDeepLink
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.toast.ToastMessage
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.result.LocalResultEventBus
import com.mrl.pixiv.common.util.result.ResultEventBus
import com.mrl.pixiv.follow.FollowingScreen
import com.mrl.pixiv.history.HistoryScreen
import com.mrl.pixiv.login.LoginOptionScreen
import com.mrl.pixiv.login.LoginScreen
import com.mrl.pixiv.login.oauth.OAuthLoginScreen
import com.mrl.pixiv.picture.HorizontalSwipePictureScreen
import com.mrl.pixiv.picture.PictureDeeplinkScreen
import com.mrl.pixiv.profile.detail.ProfileDetailScreen
import com.mrl.pixiv.report.ReportScreen
import com.mrl.pixiv.search.SearchScreen
import com.mrl.pixiv.search.result.SearchResultsScreen
import com.mrl.pixiv.setting.FileNameFormatScreen
import com.mrl.pixiv.setting.SettingScreen
import com.mrl.pixiv.setting.about.AboutScreen
import com.mrl.pixiv.setting.appdata.AppDataScreen
import com.mrl.pixiv.setting.block.BlockSettingsScreen
import com.mrl.pixiv.setting.download.DownloadScreen
import com.mrl.pixiv.setting.network.NetworkSettingScreen
import com.mrl.pixiv.splash.SplashViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf


@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun Navigation3MainGraph(
    startDestination: Destination,
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject { parametersOf(arrayOf(startDestination)) }
) {
    val listDetailStrategy = rememberListDetailSceneStrategy<Any>()
    val toastState = rememberToasterState()
    val resultBus = remember { ResultEventBus() }

    HandleDeeplink(navigationManager)
    LogScreen(navigationManager)
    SharedTransitionLayout {
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this,
            LocalToaster provides toastState,
            LocalResultEventBus provides resultBus,
        ) {
            ToastMessage(toastState = toastState)
            NavDisplay(
                backStack = navigationManager.backStack,
                modifier = modifier,
                entryDecorators = listOf(
                    // Add the default decorators for managing scenes and saving state
                    rememberSaveableStateHolderNavEntryDecorator(),
                    // Then add the view model store decorator
                    rememberViewModelStoreNavEntryDecorator()
                ),
                sceneStrategy = listDetailStrategy,
                entryProvider = entryProvider {
                    entry<Destination.LoginOption> {
                        LoginOptionScreen()
                    }
                    // 登陆
                    entry<Destination.Login> {
                        val startUrl = it.startUrl
                        LoginScreen(startUrl = startUrl)
                    }
                    // OAuth token登陆
                    entry<Destination.OAuthLogin> {
                        OAuthLoginScreen()
                    }

                    entry<Destination.Main> {
                        MainScreen()
                    }

                    // 详情页
                    entry<Destination.ProfileDetail>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) {
                        ProfileDetailScreen(
                            uid = it.userId
                        )
                    }

                    // 作品详情页（深度链接）
                    entry<Destination.PictureDeeplink>(
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
                    entry<Destination.Search> {
                        SearchScreen()
                    }

                    // 搜索结果页
                    entry<Destination.SearchResults>(
                        metadata = ListDetailSceneStrategy.listPane()
                    ) {
                        val searchWord = it.searchWords
                        SearchResultsScreen(
                            searchWords = searchWord,
                        )
                    }

                    // 设置页
                    entry<Destination.Setting>(
                        metadata = ListDetailSceneStrategy.listPane()
                    ) {
                        SettingScreen()
                    }

                    // 网络设置页
                    entry<Destination.NetworkSetting>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) {
                        NetworkSettingScreen()
                    }

                    // 保存格式设置
                    entry<Destination.FileNameFormat>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) {
                        FileNameFormatScreen()
                    }

                    // 历史记录
                    entry<Destination.History>(
                        metadata = ListDetailSceneStrategy.listPane()
                    ) {
                        HistoryScreen()
                    }

                    // 本人收藏页
                    entry<Destination.Collection>(
                        metadata = ListDetailSceneStrategy.listPane()
                    ) {
                        val userId = it.userId
                        CollectionScreen(uid = userId)
                    }

                    // 收藏标签页
                    entry<Destination.BookmarkedTags>(
                        metadata = ListDetailSceneStrategy.listPane()
                    ) {
                        BookmarkedTagsScreen()
                    }

                    entry<Destination.Following> {
                        val uid = it.userId
                        FollowingScreen(uid = uid)
                    }

                    // 横向滑动作品详情页
                    entry<Destination.Picture>(
                        metadata = ListDetailSceneStrategy.detailPane() +
                                NavDisplay.transitionSpec {
                                    scaleIn(
                                        DefaultFloatAnimationSpec,
                                        initialScale = 0.9f
                                    ) + fadeIn(
                                        DefaultFloatAnimationSpec
                                    ) togetherWith scaleOut(
                                        DefaultFloatAnimationSpec,
                                        targetScale = 1.1f
                                    ) + fadeOut(DefaultFloatAnimationSpec)
                                } +
                                NavDisplay.predictivePopTransitionSpec {
                                    scaleIn(
                                        DefaultFloatAnimationSpec,
                                        initialScale = 1.1f
                                    ) + fadeIn(
                                        DefaultFloatAnimationSpec
                                    ) togetherWith scaleOut(
                                        DefaultFloatAnimationSpec,
                                        0.9f
                                    ) + fadeOut(DefaultFloatAnimationSpec)
                                }
                    ) {
                        val illusts = remember { IllustCacheRepo[it.prefix] }
                        CompositionLocalProvider(
                            LocalSharedKeyPrefix provides it.prefix
                        ) {
                            HorizontalSwipePictureScreen(
                                illusts = illusts.toImmutableList(),
                                index = it.index,
                                prefix = it.prefix,
                                enableTransition = it.enableTransition,
                            )
                        }
                    }

                    entry<Destination.UserArtwork> {
                        ArtworkScreen(
                            userId = it.userId,
                        )
                    }
                    entry<Destination.BlockSettings> {
                        BlockSettingsScreen()
                    }
                    entry<Destination.BlockComments> {
                        BlockCommentsScreen()
                    }
                    entry<Destination.AppData> {
                        AppDataScreen()
                    }
                    entry<Destination.Download> {
                        DownloadScreen()
                    }
                    entry<Destination.About> {
                        AboutScreen()
                    }
                    entry<Destination.Comment> {
                        CommentScreen(
                            id = it.id,
                            type = it.type,
                        )
                    }
                    entry<Destination.Report> {
                        ReportScreen(
                            id = it.id,
                            type = it.type,
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ToastMessage(toastState: ToasterState) {
    LaunchedEffect(Unit) {
        ToastUtil.toastFlow.collect {
            toastState.show(it)
        }
    }
    Toaster(
        state = toastState,
        darkTheme = isSystemInDarkTheme(),
        richColors = true,
        alignment = Alignment.TopCenter,
        showCloseButton = true,
        messageSlot = {
            val contentColor = LocalToastContentColor.current
            when (val message = it.message) {
                is String -> BasicText(text = message, color = { contentColor })
                is Int -> BasicText(text = stringResource(message), color = { contentColor })
                // todo
//                is ToastMessage.Resource -> BasicText(
//                    text = stringResource(message.resId, *message.args),
//                    color = { contentColor }
//                )

                is ToastMessage.Compose -> message.content()
                else -> BasicText(text = it.message.toString(), color = { contentColor })
            }
        }
    )
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
                        Destination.PictureDeeplink(
                            data.lastPathSegment?.toLong() ?: 0
                        )
                    )
                }

                DestinationsDeepLink.userRegex.matches(data.toString()) -> {
                    navigationManager.navigate(
                        Destination.ProfileDetail(
                            data.lastPathSegment?.toLong() ?: 0
                        )
                    )
                }
            }
        }
    }
}

@OptIn(InternalSerializationApi::class)
@Composable
private fun LogScreen(
    navigationManager: NavigationManager,
) {
    LaunchedEffect(navigationManager.currentDestination) {
        // Get current destination
        val currentDestination = navigationManager.currentDestination

        // Log screen view event
        logEvent("screen_view", buildMap {
            val screenName = currentDestination::class.serializer().descriptor.serialName
                .split(".")
                .lastOrNull()
                .orEmpty()
            put("screen_name", screenName)
            put("screen_class", screenName)

            // Add additional parameters for specific destinations
            when (currentDestination) {
                is Destination.Main -> {
                    val screenName =
                        navigationManager.currentMainPage::class.serializer().descriptor.serialName
                            .split(".")
                            .lastOrNull()
                            .orEmpty()
                    put("current_main_page", screenName)
                }

                is Destination.ProfileDetail -> {
                    put("user_id", currentDestination.userId.toString())
                }

                is Destination.PictureDeeplink -> {
                    put("illust_id", currentDestination.illustId.toString())
                }

                is Destination.SearchResults -> {
                    put("search_words", currentDestination.searchWords)
                }

                is Destination.Picture -> {
                    put("index", currentDestination.index.toString())
                    put("prefix", currentDestination.prefix)
                }

                is Destination.Collection -> {
                    put("user_id", currentDestination.userId.toString())
                }

                is Destination.BookmarkedTags -> Unit

                is Destination.Following -> {
                    put("user_id", currentDestination.userId.toString())
                }

                is Destination.UserArtwork -> {
                    put("user_id", currentDestination.userId.toString())
                }

                is Destination.Login, Destination.LoginOption, Destination.OAuthLogin,
                Destination.Search, Destination.Setting, Destination.NetworkSetting,
                Destination.History, Destination.BlockSettings, Destination.AppData, Destination.Download, Destination.FileNameFormat, Destination.About -> Unit
            }
        })
    }
}
