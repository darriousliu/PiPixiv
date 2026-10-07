package com.mrl.pixiv.latest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.analytics.logEvent
import com.mrl.pixiv.common.compose.layout.currentPaneLayoutInfo
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastMedium
import com.mrl.pixiv.common.compose.ui.BackToTopButton
import com.mrl.pixiv.common.compose.ui.ViewModeToggleButton
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.kts.VSpacer
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.requireUserInfoFlow
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.reading_manga_watchlist
import com.mrl.pixiv.strings.collection
import com.mrl.pixiv.strings.latest_tab_following
import com.mrl.pixiv.strings.latest_tab_trend
import com.mrl.pixiv.strings.novel_new
import com.mrl.pixiv.strings.novel_watchlist
import kotlinx.coroutines.flow.MutableSharedFlow
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


    LaunchedEffect(pagerState.currentPage, pages) {
        logEvent("screen_view", buildMap {
            put("screen_name", "Latest")
            put("page_name", page.name)
        })
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            Column {
                BackToTopButton(
                    visibility = scrollState.canScrollBackward,
                    modifier = Modifier,
                    onBackToTop = {
                        when (scrollState) {
                            is LazyListState -> scope.launch { scrollState.scrollToItem(0) }
                            is LazyGridState -> scope.launch { scrollState.scrollToItem(0) }
                            is LazyStaggeredGridState -> scope.launch { scrollState.scrollToItem(0) }
                        }
                    },
                    onRefresh = {
                        scope.launch {
                            refreshFlow.emit(page)
                        }
                    }
                )
                8.VSpacer
                ViewModeToggleButton(
                    currentMode = appViewMode,
                    onModeChange = viewModel::switchViewMode
                )
            }
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
    ) {
        Column(modifier = Modifier.padding(it)) {
            key(appViewMode) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    edgePadding = 0.dp,
                    minTabWidth = 48.dp,
                ) {
                    pages.forEachIndexed { index, page ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
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
}
