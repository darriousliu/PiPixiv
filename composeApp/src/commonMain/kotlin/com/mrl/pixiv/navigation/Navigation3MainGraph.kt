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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.separatingVerticalHingeBounds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.mrl.pixiv.common.analytics.logScreenView
import com.mrl.pixiv.common.analytics.recordCrashScreen
import com.mrl.pixiv.common.animation.DefaultFloatAnimationSpec
import com.mrl.pixiv.common.compose.LocalSharedTransitionScope
import com.mrl.pixiv.common.compose.LocalToaster
import com.mrl.pixiv.common.compose.layout.LocalMainNavigationBottomInset
import com.mrl.pixiv.common.compose.layout.PaneInputScope
import com.mrl.pixiv.common.compose.layout.PaneInputState
import com.mrl.pixiv.common.compose.layout.SplitPaneDividerWidth
import com.mrl.pixiv.common.compose.layout.rememberSplitPaneState
import com.mrl.pixiv.common.compose.listener.EscBackHandler
import com.mrl.pixiv.common.repository.IllustCacheRepo
import com.mrl.pixiv.common.repository.recovery.DataRecoveryRepository
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.LocalNavigationManager
import com.mrl.pixiv.common.router.MainPage
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.router.paneSpec
import com.mrl.pixiv.common.router.rememberNavigationState
import com.mrl.pixiv.common.toast.ToastMessage
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.result.LocalResultEventBus
import com.mrl.pixiv.common.util.result.ResultEventBus
import com.mrl.pixiv.community.CommunityUsersScreen
import com.mrl.pixiv.community.NotificationScreen
import com.mrl.pixiv.follow.FollowingScreen
import com.mrl.pixiv.history.HistoryScreen
import com.mrl.pixiv.image.preview.ImagePreviewScreen
import com.mrl.pixiv.login.LoginOptionScreen
import com.mrl.pixiv.login.LoginScreen
import com.mrl.pixiv.login.oauth.OAuthLoginScreen
import com.mrl.pixiv.login.oauth.WebCookieLoginScreen
import com.mrl.pixiv.manga.MangaSeriesScreen
import com.mrl.pixiv.manga.MangaWatchlistScreen
import com.mrl.pixiv.manga.UserMangaSeriesScreen
import com.mrl.pixiv.novel.NovelScreen
import com.mrl.pixiv.novel.readlater.NovelReadLaterScreen
import com.mrl.pixiv.novel.series.NovelSeriesScreen
import com.mrl.pixiv.picture.HorizontalSwipePictureScreen
import com.mrl.pixiv.picture.PictureDeeplinkScreen
import com.mrl.pixiv.profile.detail.ProfileDetailScreen
import com.mrl.pixiv.profile.marker.NovelMarkersScreen
import com.mrl.pixiv.profile.reading.ReadingScreen
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
import com.mrl.pixiv.setting.account.AccountManagementScreen
import com.mrl.pixiv.setting.ai.AiTranslationSettingScreen
import com.mrl.pixiv.setting.appdata.AppDataScreen
import com.mrl.pixiv.setting.appearance.AppearanceSettingScreen
import com.mrl.pixiv.setting.applog.AppLogScreen
import com.mrl.pixiv.setting.block.BlockIllustScreen
import com.mrl.pixiv.setting.block.BlockNovelScreen
import com.mrl.pixiv.setting.block.BlockSettingsScreen
import com.mrl.pixiv.setting.block.BlockTagScreen
import com.mrl.pixiv.setting.block.BlockUserScreen
import com.mrl.pixiv.setting.block.CloudMuteScreen
import com.mrl.pixiv.setting.download.DownloadScreen
import com.mrl.pixiv.setting.network.NetworkDiagnosticScreen
import com.mrl.pixiv.setting.network.NetworkSettingScreen
import com.mrl.pixiv.setting.recovery.DataRecoveryScreen
import com.mrl.pixiv.splash.StartupMigration
import com.mrl.pixiv.strings.data_recovery_view
import com.mrl.pixiv.strings.recovery_pending_notice
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf


@Composable
fun Navigation3MainGraph(
    startDestination: Destination,
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject { parametersOf(arrayOf(startDestination)) }
) {
    val toastState = rememberToasterState()
    val resultBus = remember { ResultEventBus() }
    val recovery: DataRecoveryRepository = koinInject()

    rememberNavigationState(navigationManager)
    val inputState = remember { PaneInputState() }
    val paneTransitionState = remember { AdaptivePaneTransitionState() }
    val splitState = rememberSplitPaneState()
    val topRecord = navigationManager.backStack.last()
    val sourceRecord = navigationManager.backStack.lastOrNull {
        it.entryId == topRecord.ownerEntryId
    } ?: topRecord
    val showMainNavigation =
        navigationManager.backStack.any { it.destination == Destination.Main } &&
                !sourceRecord.destination.paneSpec.preferredFullWidth
    LaunchedEffect(topRecord.entryId) {
        inputState.activeEntryId = topRecord.entryId
        inputState.dividerFocused = false
    }
    val pictureCacheDecorator = rememberPictureCacheDecorator(navigationManager)
    val destinationEntryProvider = entryProvider {
        entry<Destination.LoginOption> {
            LoginOptionScreen()
        }
        entry<Destination.AddAccount> {
            LoginOptionScreen(addAccount = true)
        }
        // 登陆
        entry<Destination.Login> {
            LoginScreen(startUrl = it.startUrl, addAccount = it.addAccount)
        }
        // OAuth token登陆
        entry<Destination.OAuthLogin> {
            OAuthLoginScreen()
        }

        entry<Destination.WebCookieLogin> {
            WebCookieLoginScreen()
        }

        entry<Destination.Main> {
            val migration: StartupMigration = koinInject()
            if (navigationManager.currentDestination == Destination.Main) {
                LaunchedEffect(migration) { migration.onHomeEntered() }
            }
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
                sharedImage = it.sharedImage,
                onBack = currentNavigationManager()::popBackStack,
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

        entry<Destination.AccountManagement> {
            AccountManagementScreen()
        }

        // 网络设置页
        entry<Destination.NetworkSetting> {
            NetworkSettingScreen()
        }

        entry<Destination.NetworkDiagnostic> {
            NetworkDiagnosticScreen()
        }

        entry<Destination.AppearanceSetting> {
            AppearanceSettingScreen()
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

        entry<Destination.Reading> {
            ReadingScreen()
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
            val illusts = remember(it.cacheKey) { IllustCacheRepo[it.cacheKey].toImmutableList() }
            HorizontalSwipePictureScreen(
                illusts = illusts,
                index = it.index,
                cover = it.cover,
            )
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
            BlockSettingsScreen(
                blockedCommentsContent = { modifier ->
                    BlockCommentsScreen(modifier = modifier, embedded = true)
                },
            )
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
        entry<Destination.DataRecovery> { destination ->
            DataRecoveryScreen(
                afterLogin = destination.afterLogin,
                onBack = currentNavigationManager()::popBackStack,
            )
        }
        entry<Destination.Download> {
            DownloadScreen()
        }
        entry<Destination.About> {
            AboutScreen()
        }

        entry<Destination.AppLog> {
            AppLogScreen()
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
        entry<Destination.MangaSeries> {
            MangaSeriesScreen(seriesId = it.seriesId)
        }
        entry<Destination.MangaWatchlist> {
            MangaWatchlistScreen()
        }
        entry<Destination.UserMangaSeries> {
            UserMangaSeriesScreen(userId = it.userId)
        }
        entry<Destination.Notifications> { destination ->
            val entryNavigation = currentNavigationManager()
            NotificationScreen(
                notificationId = destination.notificationId,
                title = destination.title,
                onViewMore = { id, title -> entryNavigation.navigateToNotifications(id, title) },
            )
        }
        entry<Destination.CommunityUsers> {
            CommunityUsersScreen(kind = it.kind, seedUserId = it.seedUserId)
        }
        entry<Destination.CloudMute> {
            CloudMuteScreen()
        }
    }

    HandleDeeplink(navigationManager)
    LogScreen(navigationManager)
    EscBackHandler(navigationManager::popBackStack)
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
                    val allowSplit =
                        adaptiveInfo.windowPosture.separatingVerticalHingeBounds.isEmpty()
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
                    val strategy = remember(
                        maxWidth,
                        maxHeight,
                        allowSplit,
                        splitState,
                        inputState,
                        paneTransitionState
                    ) {
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
                        // 快速返回到共享图片的来源页时，仍需播完共享元素的收尾动画。
                        settlesQuickBack = { scene, previous -> scene.returnsWithSharedImage(previous) },
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
                    HomeRecoveryNotice(
                        visible = topRecord.destination == Destination.Main &&
                                navigationManager.currentMainPage == MainPage.Home,
                        recovery = recovery,
                        onView = { navigationManager.navigate(Destination.DataRecovery()) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = LocalMainNavigationBottomInset.current),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeRecoveryNotice(
    visible: Boolean,
    recovery: DataRecoveryRepository,
    onView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasPending by recovery.hasPending.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var shown by rememberSaveable { mutableStateOf(false) }
    val message = stringResource(RStrings.recovery_pending_notice)
    val viewLabel = stringResource(RStrings.data_recovery_view)
    LaunchedEffect(visible, hasPending) {
        if (!visible || !hasPending || shown) return@LaunchedEffect
        shown = true
        if (snackbar.showSnackbar(message, viewLabel, withDismissAction = true) ==
            SnackbarResult.ActionPerformed
        ) {
            onView()
        }
    }
    if (visible && hasPending) SnackbarHost(snackbar, modifier = modifier)
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

@Composable
private fun LogScreen(
    navigationManager: NavigationManager,
) {
    val destination = navigationManager.currentDestination
    val mainPage = if (destination == Destination.Main) navigationManager.currentMainPage else null
    LaunchedEffect(navigationManager.topEntryId, mainPage) {
        val screen = mainPage ?: destination
        val name = screenName(screen)
        recordCrashScreen(name)
        when (screen) {
            is MainPage.Home,
            is MainPage.Ranking,
            is MainPage.Latest,
            is Destination.Following,
            is Destination.SearchResults -> Unit

            else -> logScreenView(name)
        }
    }
}

@OptIn(InternalSerializationApi::class)
private fun screenName(screen: Any): String =
    screen::class.serializer().descriptor.serialName.substringAfterLast('.')
