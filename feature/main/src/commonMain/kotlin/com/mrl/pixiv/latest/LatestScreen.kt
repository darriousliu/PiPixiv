package com.mrl.pixiv.latest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.mrl.pixiv.MainPageFloatingActions
import com.mrl.pixiv.MainPageScrollableTabRow
import com.mrl.pixiv.common.analytics.trackTab
import com.mrl.pixiv.common.compose.layout.currentPaneLayoutInfo
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastMedium
import com.mrl.pixiv.common.compose.ui.BackToTopButton
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.requireUserInfoFlow
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.collection
import com.mrl.pixiv.strings.latest_tab_following
import com.mrl.pixiv.strings.latest_tab_trend
import com.mrl.pixiv.strings.new_artworks
import com.mrl.pixiv.strings.novel_new
import com.mrl.pixiv.strings.novel_watchlist
import com.mrl.pixiv.strings.reading_manga_watchlist
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LatestScreen(
    modifier: Modifier = Modifier,
    viewModel: LatestViewModel = koinViewModel(),
) {
    val userInfo by requireUserInfoFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val paneSizeClass = currentPaneLayoutInfo().sizeClass
    val refreshFlow = remember { MutableSharedFlow<LatestPage>() }
    val isWidthAtLeastMedium = paneSizeClass.isWidthAtLeastMedium
    val appViewMode by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle { appViewMode }
    val pages = remember(appViewMode) { LatestPage.pagesFor(appViewMode) }
    val pagerState = viewModel.pagerStateFor(appViewMode)
    val page = pages[pagerState.currentPage.coerceIn(pages.indices)]
    val scrollState = when (page) {
        LatestPage.Trend -> when (appViewMode) {
            AppViewMode.ILLUST -> viewModel.trendingLazyGirdState
            AppViewMode.NOVEL -> viewModel.trendingNovelLazyListState
        }

        LatestPage.Collection -> when (appViewMode) {
            AppViewMode.ILLUST -> viewModel.collectionLazyGirdState
            AppViewMode.NOVEL -> viewModel.collectionNovelLazyListState
        }

        LatestPage.Following -> when (appViewMode) {
            AppViewMode.ILLUST -> if (isWidthAtLeastMedium) {
                viewModel.followingLazyGirdState
            } else {
                viewModel.followingLazyListState
            }

            AppViewMode.NOVEL -> if (isWidthAtLeastMedium) {
                viewModel.followingLazyGirdState
            } else {
                viewModel.followingLazyListState
            }
        }

        LatestPage.NovelNew -> viewModel.newNovelLazyListState
        LatestPage.NovelWatchlist -> viewModel.watchlistNovelLazyListState
        LatestPage.MangaWatchlist -> viewModel.watchlistMangaLazyListState
    }

    LaunchedEffect(lifecycleOwner, pagerState, pages) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { pages[pagerState.currentPage.coerceIn(pages.indices)] }
                .collectLatest { visiblePage ->
                    trackTab("Latest", visiblePage)
                }
        }
    }

    LatestScreenContent(
        appViewMode = appViewMode,
        selectedIndex = pagerState.currentPage,
        onSelectPage = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
        onModeChange = viewModel::switchViewMode,
        canScrollBackward = scrollState.canScrollBackward,
        onBackToTop = {
            when (scrollState) {
                is LazyListState -> scrollState.scrollToItem(0)
                is LazyGridState -> scrollState.scrollToItem(0)
                is LazyStaggeredGridState -> scrollState.scrollToItem(0)
            }
        },
        onRefresh = { scope.launch { refreshFlow.emit(page) } },
        modifier = modifier,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { index ->
            val page = pages[index]
            when (page) {
                LatestPage.Trend -> {
                    TrendingPage(refreshFlow = refreshFlow)
                }

                LatestPage.Collection -> {
                    CollectionPage(
                        uid = userInfo.user.id,
                        refreshFlow = refreshFlow
                    )
                }

                LatestPage.Following -> {
                    FollowingPage(
                        uid = userInfo.user.id,
                        refreshFlow = refreshFlow
                    )
                }

                LatestPage.NovelNew -> {
                    NewNovelPage(refreshFlow = refreshFlow)
                }

                LatestPage.NovelWatchlist -> {
                    NovelWatchlistPage(refreshFlow = refreshFlow)
                }

                LatestPage.MangaWatchlist -> {
                    MangaWatchlistPage(refreshFlow = refreshFlow)
                }
            }
        }
    }
}

@Composable
internal fun LatestScreenContent(
    appViewMode: AppViewMode,
    selectedIndex: Int,
    onSelectPage: (Int) -> Unit,
    onModeChange: (AppViewMode) -> Unit,
    canScrollBackward: Boolean,
    onBackToTop: suspend () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    // 框架调整时同步 feature/setting/.../appearance/MainPageSkeletons.kt 中的 LatestSkeleton。
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(RStrings.new_artworks)) }) },
        floatingActionButton = {
            MainPageFloatingActions(currentMode = appViewMode, onModeChange = onModeChange) {
                BackToTopButton(
                    visibility = canScrollBackward,
                    onBackToTop = onBackToTop,
                    onRefresh = onRefresh,
                )
            }
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            key(appViewMode) {
                LatestTabs(
                    pages = LatestPage.pagesFor(appViewMode),
                    selectedIndex = selectedIndex,
                    onSelect = onSelectPage,
                )
            }
            content()
        }
    }
}

@Composable
internal fun LatestTabs(pages: List<LatestPage>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    MainPageScrollableTabRow(selectedIndex = selectedIndex) {
        pages.forEachIndexed { index, page ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                text = {
                    Text(
                        text = stringResource(
                            when (page) {
                                LatestPage.Trend -> RStrings.latest_tab_trend
                                LatestPage.Collection -> RStrings.collection
                                LatestPage.Following -> RStrings.latest_tab_following
                                LatestPage.NovelNew -> RStrings.novel_new
                                LatestPage.NovelWatchlist -> RStrings.novel_watchlist
                                LatestPage.MangaWatchlist -> RStrings.reading_manga_watchlist
                            }
                        ),
                        maxLines = 1,
                        softWrap = false,
                    )
                },
            )
        }
    }
}
