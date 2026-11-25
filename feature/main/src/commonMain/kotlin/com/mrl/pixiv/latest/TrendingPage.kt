package com.mrl.pixiv.latest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.compose.RecommendGridDefaults
import com.mrl.pixiv.common.compose.layout.AdaptiveVerticalStaggeredGrid
import com.mrl.pixiv.common.compose.layout.LocalMainNavigationBottomInset
import com.mrl.pixiv.common.compose.layout.withMainNavigationBottomInset
import com.mrl.pixiv.common.compose.listener.KeyEventListener
import com.mrl.pixiv.common.compose.listener.keyboardScrollerController
import com.mrl.pixiv.common.compose.ui.ContentPullToRefreshBox
import com.mrl.pixiv.common.compose.ui.PagingAppendState
import com.mrl.pixiv.common.compose.ui.PagingContentState
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.compose.ui.illust.RectangleIllustItem
import com.mrl.pixiv.common.compose.ui.novel.NovelItem
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.kts.itemIndexKey
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import kotlinx.coroutines.flow.SharedFlow
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TrendingPage(
    refreshFlow: SharedFlow<LatestPage>,
    modifier: Modifier = Modifier,
    viewModel: LatestViewModel = koinViewModel(),
) {
    val navigationManager = currentNavigationManager()
    val appViewMode by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle { appViewMode }

    when (appViewMode) {
        AppViewMode.ILLUST -> {
            TrendingIllustPage(
                refreshFlow = refreshFlow,
                modifier = modifier,
                viewModel = viewModel,
                navigationManager = navigationManager
            )
        }

        AppViewMode.NOVEL -> {
            TrendingNovelPage(
                refreshFlow = refreshFlow,
                modifier = modifier,
                viewModel = viewModel,
                navigationManager = navigationManager
            )
        }
    }
}

@Composable
private fun TrendingIllustPage(
    refreshFlow: SharedFlow<LatestPage>,
    modifier: Modifier = Modifier,
    viewModel: LatestViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val pullRefreshState = rememberPullToRefreshState()
    val illustsFollowing = viewModel.illustsFollowing.collectAsLazyPagingItems()
    val trendingFilter by viewModel.trendingFilter.collectAsStateWithLifecycle()
    val layoutParams = RecommendGridDefaults.coverLayoutParameters()
    val isRefreshing = illustsFollowing.loadState.refresh is LoadState.Loading
    val lazyGridState = viewModel.trendingLazyGirdState
    val controller = remember {
        keyboardScrollerController(lazyGridState) {
            lazyGridState.layoutInfo.viewportSize.height.toFloat()
        }
    }

    KeyEventListener(controller)

    LaunchedEffect(Unit) {
        refreshFlow.collect {
            illustsFollowing.refresh()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LatestFilterRow(
            selected = trendingFilter,
            onSelect = { restrict ->
                viewModel.updateRestrict(restrict)
                illustsFollowing.refresh()
            },
            showAll = true,
        )
        ContentPullToRefreshBox(
            hasContent = illustsFollowing.itemCount > 0,
            isRefreshing = isRefreshing,
            onRefresh = { illustsFollowing.refresh() },
            modifier = Modifier.fillMaxSize(),
            state = pullRefreshState,
        ) {
            AdaptiveVerticalStaggeredGrid(
                columns = layoutParams.gridCells,
                modifier = Modifier.fillMaxSize(),
                state = lazyGridState,
                contentPadding = latestArtworkGridPadding(layoutParams.edgePadding),
                verticalItemSpacing = layoutParams.verticalArrangement.spacing,
                horizontalArrangement = layoutParams.horizontalArrangement,
            ) {
                items(
                    count = illustsFollowing.itemCount,
                    key = illustsFollowing.itemIndexKey { index, item -> "${index}_${item.id}" }
                ) { index ->
                    val illust = illustsFollowing[index] ?: return@items
                    val isBookmarked = illust.isBookmark
                    RectangleIllustItem(
                        illust = illust,
                        isBookmarked = isBookmarked,
                        navToPictureScreen = { cover ->
                            navigationManager.navigateToPictureScreen(
                                illustsFollowing.itemSnapshotList.items,
                                index,
                                cover,
                            )
                        },
                        onBookmarkClick = { restrict, tags, isEdit ->
                            if (isEdit || !isBookmarked) {
                                BookmarkState.bookmarkIllust(illust.id, restrict, tags)
                            } else {
                                BookmarkState.deleteBookmarkIllust(illust.id)
                            }
                        },
                        sharedElementSlot = "latest-trending:$index",
                    )
                }

                if (
                    illustsFollowing.itemCount > 0 &&
                    illustsFollowing.loadState.refresh is LoadState.NotLoading &&
                    illustsFollowing.loadState.append !is LoadState.NotLoading
                ) {
                    item(key = "append_state", span = StaggeredGridItemSpan.FullLine) {
                        PagingAppendState(
                            loadStates = illustsFollowing.loadState,
                            itemCount = illustsFollowing.itemCount,
                            onRetry = illustsFollowing::retry,
                            modifier = Modifier.padding(bottom = 136.dp),
                        )
                    }
                }
            }
            PagingContentState(
                loadStates = illustsFollowing.loadState,
                itemCount = illustsFollowing.itemCount,
                onRetry = illustsFollowing::retry,
                modifier = Modifier.padding(
                    bottom = LocalMainNavigationBottomInset.current +
                            if (illustsFollowing.itemCount > 0) 136.dp else 0.dp,
                ),
            )
            VerticalScrollbar(
                state = lazyGridState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun TrendingNovelPage(
    refreshFlow: SharedFlow<LatestPage>,
    modifier: Modifier = Modifier,
    viewModel: LatestViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val pullRefreshState = rememberPullToRefreshState()
    val novelsFollowing = viewModel.novelsFollowing.collectAsLazyPagingItems()
    val trendingFilter by viewModel.trendingFilter.collectAsStateWithLifecycle()
    val isRefreshing = novelsFollowing.loadState.refresh is LoadState.Loading
    val lazyListState = viewModel.trendingNovelLazyListState
    val controller = remember {
        keyboardScrollerController(lazyListState) {
            lazyListState.layoutInfo.viewportSize.height.toFloat()
        }
    }

    KeyEventListener(controller)

    LaunchedEffect(Unit) {
        refreshFlow.collect {
            novelsFollowing.refresh()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LatestFilterRow(
            selected = trendingFilter,
            onSelect = { restrict ->
                viewModel.updateRestrict(restrict)
                novelsFollowing.refresh()
            },
            showAll = true,
        )
        ContentPullToRefreshBox(
            hasContent = novelsFollowing.itemCount > 0,
            isRefreshing = isRefreshing,
            onRefresh = { novelsFollowing.refresh() },
            modifier = Modifier.fillMaxSize(),
            state = pullRefreshState,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = PaddingValues().withMainNavigationBottomInset(),
            ) {
                items(
                    count = novelsFollowing.itemCount,
                    key = novelsFollowing.itemIndexKey { index, item -> "${index}_${item.id}" }
                ) { index ->
                    novelsFollowing[index]?.let { novel ->
                        NovelItem(
                            novel = novel,
                            onNovelClick = { novelId ->
                                navigationManager.navigateToNovelDetailScreen(novelId)
                            },
                            onSeriesClick = navigationManager::navigateToNovelSeriesScreen,
                            onBookmarkClick = { isBookmarked, restrict, tags ->
                                if (isBookmarked) {
                                    BookmarkState.deleteBookmarkNovel(novel.id)
                                } else {
                                    BookmarkState.bookmarkNovel(novel.id, restrict, tags)
                                }
                            }
                        )
                    }
                }

                if (
                    novelsFollowing.itemCount > 0 &&
                    novelsFollowing.loadState.refresh is LoadState.NotLoading &&
                    novelsFollowing.loadState.append !is LoadState.NotLoading
                ) {
                    item(key = "append_state") {
                        PagingAppendState(
                            loadStates = novelsFollowing.loadState,
                            itemCount = novelsFollowing.itemCount,
                            onRetry = novelsFollowing::retry,
                            modifier = Modifier.padding(bottom = 136.dp),
                        )
                    }
                }
            }
            PagingContentState(
                loadStates = novelsFollowing.loadState,
                itemCount = novelsFollowing.itemCount,
                onRetry = novelsFollowing::retry,
                modifier = Modifier.padding(
                    bottom = LocalMainNavigationBottomInset.current +
                            if (novelsFollowing.itemCount > 0) 136.dp else 0.dp,
                ),
            )
            VerticalScrollbar(
                state = lazyListState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}
