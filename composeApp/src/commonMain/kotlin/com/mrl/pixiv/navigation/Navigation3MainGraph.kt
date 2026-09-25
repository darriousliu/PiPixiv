package com.mrl.pixiv.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.separatingVerticalHingeBounds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dokar.sonner.LocalToastContentColor
import com.dokar.sonner.Toaster
import com.dokar.sonner.ToasterState
import com.dokar.sonner.rememberToasterState
import com.mrl.pixiv.MainNavigationScaffold
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
import com.mrl.pixiv.common.compose.layout.PaneInputScope
import com.mrl.pixiv.common.compose.layout.PaneInputState
import com.mrl.pixiv.common.compose.layout.SplitPaneDividerWidth
import com.mrl.pixiv.common.compose.layout.rememberSplitPaneState
import com.mrl.pixiv.common.compose.listener.EscBackHandler
import com.mrl.pixiv.common.repository.IllustCacheRepo
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.LocalNavigationManager
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.paneSpec
import com.mrl.pixiv.common.router.rememberNavigationState
import com.mrl.pixiv.common.toast.ToastMessage
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.result.LocalResultEventBus
import com.mrl.pixiv.common.util.result.ResultEventBus
import com.mrl.pixiv.follow.FollowingScreen
import com.mrl.pixiv.history.HistoryScreen
import com.mrl.pixiv.image.preview.ImagePreviewScreen
import com.mrl.pixiv.login.LoginOptionScreen
import com.mrl.pixiv.login.LoginScreen
import com.mrl.pixiv.login.oauth.OAuthLoginScreen
import com.mrl.pixiv.login.oauth.WebCookieLoginScreen
import com.mrl.pixiv.novel.NovelScreen
import com.mrl.pixiv.novel.readlater.NovelReadLaterScreen
import com.mrl.pixiv.novel.series.NovelSeriesScreen
import com.mrl.pixiv.picture.HorizontalSwipePictureScreen
import com.mrl.pixiv.picture.PictureDeeplinkScreen
import com.mrl.pixiv.profile.detail.ProfileDetailScreen
import com.mrl.pixiv.profile.marker.NovelMarkersScreen
import com.mrl.pixiv.report.ReportScreen
import com.mrl.pixiv.search.SearchScreen
import com.mrl.pixiv.search.result.SearchResultsScreen
import com.mrl.pixiv.setting.BrowsingSettingScreen
import com.mrl.pixiv.setting.FileNameFormatScreen
import com.mrl.pixiv.setting.HistorySettingScreen
import com.mrl.pixiv.setting.PrivacySettingScreen
import com.mrl.pixiv.setting.SearchSettingScreen
import com.mrl.pixiv.setting.SettingScreen
import com.mrl.pixiv.setting.about.AboutScreen
import com.mrl.pixiv.setting.ai.AiTranslationSettingScreen
import com.mrl.pixiv.setting.appdata.AppDataScreen
import com.mrl.pixiv.setting.block.BlockIllustScreen
import com.mrl.pixiv.setting.block.BlockNovelScreen
import com.mrl.pixiv.setting.block.BlockSettingsScreen
import com.mrl.pixiv.setting.block.BlockTagScreen
import com.mrl.pixiv.setting.block.BlockUserScreen
import com.mrl.pixiv.setting.download.DownloadScreen
import com.mrl.pixiv.setting.network.NetworkSettingScreen
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf


@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun Navigation3MainGraph(
    startDestination: Destination,
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject { parametersOf(arrayOf(startDestination)) }
) {
    val toastState = rememberToasterState()
    val resultBus = remember { ResultEventBus() }

    rememberNavigationState(navigationManager)
    val inputState = remember { PaneInputState() }
    val paneTransitionState = remember { AdaptivePaneTransitionState() }
    val splitState = rememberSplitPaneState()
    val topRecord = navigationManager.backStack.last()
    val sourceRecord = navigationManager.backStack.lastOrNull {
        it.entryId == topRecord.ownerEntryId
    } ?: topRecord
    val showMainNavigation = navigationManager.backStack.any { it.destination == Destination.Main } &&
        !sourceRecord.destination.paneSpec.preferredFullWidth
    LaunchedEffect(topRecord.entryId) {
        inputState.activeEntryId = topRecord.entryId
        inputState.dividerFocused = false
    }
    val pictureCacheDecorator = rememberPictureCacheDecorator(navigationManager)
    val destinationEntryProvider = entryProvider<Destination> {
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

        entry<Destination.WebCookieLogin> {
            WebCookieLoginScreen()
        }

        entry<Destination.Main> {
            MainScreen()
        }

        // 详情页
        entry<Destination.ProfileDetail> {
            ProfileDetailScreen(
                uid = it.userId
            )
        }

        // 作品详情页（深度链接）
        entry<Destination.PictureDeeplink>(
            metadata = NavDisplay.transitionSpec {
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

        entry<Destination.ImagePreview>(
            metadata = NavDisplay.transitionSpec {
                fadeIn(DefaultFloatAnimationSpec) togetherWith
                        fadeOut(DefaultFloatAnimationSpec)
            } + NavDisplay.predictivePopTransitionSpec {
                fadeIn(DefaultFloatAnimationSpec) togetherWith
                        fadeOut(DefaultFloatAnimationSpec)
            },
        ) {
            ImagePreviewScreen(
                imageUrls = it.imageUrls,
                initialIndex = it.initialIndex,
                sharedElementKey = it.sharedElementKey,
                onBack = navigationManager::popBackStack,
            )
        }

        // 搜索页
        entry<Destination.Search> {
            SearchScreen()
        }

        // 搜索结果页
        entry<Destination.SearchResults> {
            SearchResultsScreen(
                searchWords = it.searchWords,
                searchMode = it.searchMode,
                isIdSearch = it.isIdSearch,
            )
        }

        // 设置页
        entry<Destination.Setting> {
            SettingScreen()
        }

        // 网络设置页
        entry<Destination.NetworkSetting> {
            NetworkSettingScreen()
        }

        entry<Destination.BrowsingSetting> {
            BrowsingSettingScreen()
        }

        entry<Destination.SearchSetting> {
            SearchSettingScreen()
        }

        entry<Destination.HistorySetting> {
            HistorySettingScreen()
        }

        entry<Destination.PrivacySetting> {
            PrivacySettingScreen()
        }

        // 保存格式设置
        entry<Destination.FileNameFormat> {
            FileNameFormatScreen()
        }

        entry<Destination.AiTranslationSetting> {
            AiTranslationSettingScreen()
        }

        // 历史记录
        entry<Destination.History> {
            HistoryScreen()
        }

        entry<Destination.NovelReadLater> {
            NovelReadLaterScreen()
        }

        // 本人收藏页
        entry<Destination.Collection> {
            CollectionScreen(uid = it.userId, isNovel = it.isNovel)
        }

        // 收藏标签页
        entry<Destination.BookmarkedTags> {
            BookmarkedTagsScreen()
        }

        entry<Destination.NovelMarkers> {
            NovelMarkersScreen()
        }

        entry<Destination.Following> {
            val uid = it.userId
            FollowingScreen(uid = uid)
        }

        // 横向滑动作品详情页
        entry<Destination.Picture>(
            metadata = NavDisplay.transitionSpec {
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
                    enableTransition = it.enableTransition,
                )
            }
        }

        entry<Destination.UserArtwork> {
            ArtworkScreen(
                userId = it.userId,
                initialType = it.initialType,
            )
        }
        entry<Destination.UserNovels> {
            ArtworkScreen(
                userId = it.userId,
                initialNovel = true,
            )
        }
        entry<Destination.BlockSettings> {
            BlockSettingsScreen()
        }
        entry<Destination.BlockIllust> {
            BlockIllustScreen()
        }
        entry<Destination.BlockNovel> {
            BlockNovelScreen()
        }
        entry<Destination.BlockUser> {
            BlockUserScreen()
        }
        entry<Destination.BlockTag> {
            BlockTagScreen()
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
        entry<Destination.NovelDetail> {
            NovelScreen(
                novelId = it.novelId,
                markerPage = it.markerPage,
                readLaterTargetLanguage = it.readLaterTargetLanguage,
            )
        }
        entry<Destination.NovelSeries> {
            NovelSeriesScreen(
                seriesId = it.seriesId,
            )
        }
    }

    HandleDeeplink(navigationManager)
    LogScreen(navigationManager)
    EscBackHandler {
        if (navigationManager.backStack.isEmpty()) return@EscBackHandler
        navigationManager.popBackStack()
    }
    SharedTransitionLayout {
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this,
            LocalToaster provides toastState,
            LocalResultEventBus provides resultBus,
        ) {
            ToastMessage(toastState = toastState)
            MainNavigationScaffold(showMainNavigation, navigationManager) {
                BoxWithConstraints(modifier.fillMaxSize()) {
                    val adaptiveInfo = currentWindowAdaptiveInfoV2()
                    val allowSplit = adaptiveInfo.windowPosture.separatingVerticalHingeBounds.isEmpty()
                    val showsSidePane = allowSplit && topRecord.ownerEntryId != null &&
                        topRecord.destination.paneSpec.canShowAsDetail &&
                        !topRecord.destination.paneSpec.preferredFullWidth &&
                        sourceRecord.destination.paneSpec.canHostDetail && maxHeight >= 480.dp &&
                        maxWidth >= sourceRecord.destination.paneSpec.minSourceWidth +
                            SplitPaneDividerWidth + topRecord.destination.paneSpec.minDetailWidth
                    LaunchedEffect(showsSidePane, topRecord.entryId) {
                        if (!showsSidePane || inputState.activeEntryId !in
                            listOf(sourceRecord.entryId, topRecord.entryId)
                        ) {
                            inputState.activeEntryId = topRecord.entryId
                        }
                        if (!showsSidePane) inputState.dividerFocused = false
                    }
                    val strategy = remember(maxWidth, maxHeight, allowSplit, splitState, inputState, paneTransitionState) {
                        AdaptiveSceneStrategy(
                            availableWidth = maxWidth,
                            availableHeight = maxHeight,
                            splitState = splitState,
                            inputState = inputState,
                            paneTransitionState = paneTransitionState,
                            allowSplit = allowSplit,
                        )
                    }
                    PredictiveBackNavDisplay(
                        backStack = navigationManager.backStack,
                        modifier = Modifier.fillMaxSize(),
                        onBack = navigationManager::popBackStack,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        entryDecorators = listOf(
                            // Add the default decorators for managing scenes and saving state
                            rememberSaveableStateHolderNavEntryDecorator(),
                            // Then add the view model store decorator
                            rememberViewModelStoreNavEntryDecorator(),
                            pictureCacheDecorator,
                        ),
                        sceneStrategies = listOf(strategy),
                        entryProvider = { record ->
                            val destinationEntry = destinationEntryProvider(record.destination)
                            NavEntry(
                                key = record,
                                contentKey = record.entryId,
                                metadata = destinationEntry.metadata + metadata {
                                    put(NavigationRecordKey, record)
                                },
                            ) {
                                val scopedNavigation = remember(navigationManager, record.entryId) {
                                    navigationManager.forEntry(record.entryId)
                                }
                                CompositionLocalProvider(LocalNavigationManager provides scopedNavigation) {
                                    PaneInputScope(record.entryId, inputState) {
                                        destinationEntry.Content()
                                    }
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
                is StringResource -> BasicText(
                    text = stringResource(message),
                    color = { contentColor })

                is ToastMessage.Compose -> message.content()
                else -> BasicText(text = it.message.toString(), color = { contentColor })
            }
        }
    )
}

@Composable
internal expect fun HandleDeeplink(
    navigationManager: NavigationManager,
)

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
                is Destination.Login -> {
                    put("start_url", currentDestination.startUrl)
                }

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
                    put("is_id_search", currentDestination.isIdSearch.toString())
                    put("search_mode", currentDestination.searchMode.name)
                }

                is Destination.Picture -> {
                    put("index", currentDestination.index.toString())
                    put("prefix", currentDestination.prefix)
                    put("enable_transition", currentDestination.enableTransition.toString())
                }

                is Destination.Collection -> {
                    put("user_id", currentDestination.userId.toString())
                    put("is_novel", currentDestination.isNovel.toString())
                }

                is Destination.Following -> {
                    put("user_id", currentDestination.userId.toString())
                }

                is Destination.UserArtwork -> {
                    put("user_id", currentDestination.userId.toString())
                }

                is Destination.UserNovels -> {
                    put("user_id", currentDestination.userId.toString())
                }

                is Destination.Comment -> {
                    put("id", currentDestination.id.toString())
                    put("type", currentDestination.type.toString())
                }

                is Destination.Report -> {
                    put("id", currentDestination.id.toString())
                    put("type", currentDestination.type.toString())
                }

                is Destination.NovelDetail -> {
                    put("novel_id", currentDestination.novelId.toString())
                }

                is Destination.NovelSeries -> {
                    put("series_id", currentDestination.seriesId.toString())
                }

                else -> Unit
            }
        })
    }
}
