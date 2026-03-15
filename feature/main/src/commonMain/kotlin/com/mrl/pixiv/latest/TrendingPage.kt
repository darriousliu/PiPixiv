package com.mrl.pixiv.latest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
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
import com.mrl.pixiv.common.compose.listener.KeyEventListener
import com.mrl.pixiv.common.compose.listener.keyboardScrollerController
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.compose.ui.illust.RectangleIllustItem
import com.mrl.pixiv.common.compose.ui.novel.NovelItem
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.kts.itemIndexKey
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.all
import com.mrl.pixiv.strings.word_private
import com.mrl.pixiv.strings.word_public
import kotlinx.coroutines.flow.SharedFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val KEY_TOP_SPACE = "top_space"

@Composable
fun TrendingPage(
    refreshFlow: SharedFlow<LatestPage>,
    modifier: Modifier = Modifier,
    viewModel: LatestViewModel = koinViewModel(),
) {
    val navigationManager = koinInject<NavigationManager>()
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
    navigationManager: NavigationManager = koinInject(),
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

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { illustsFollowing.refresh() },
        modifier = modifier.fillMaxSize(),
        state = pullRefreshState,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    ) {
        Box {
            LazyVerticalStaggeredGrid(
                columns = layoutParams.gridCells,
                modifier = Modifier.fillMaxSize(),
                state = lazyGridState,
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 10.dp),
                verticalItemSpacing = layoutParams.verticalArrangement.spacing,
                horizontalArrangement = layoutParams.horizontalArrangement,
            ) {
                item(
                    key = KEY_TOP_SPACE,
                    span = StaggeredGridItemSpan.FullLine
                ) {
                    Spacer(modifier = Modifier.height(40.dp))
                }
                items(
                    count = illustsFollowing.itemCount,
                    key = illustsFollowing.itemIndexKey { index, item -> "${index}_${item.id}" }
                ) { index ->
                    val illust = illustsFollowing[index] ?: return@items
                    val isBookmarked = illust.isBookmark
                    RectangleIllustItem(
                        illust = illust,
                        isBookmarked = isBookmarked,
                        navToPictureScreen = { prefix, enableTransition ->
                            navigationManager.navigateToPictureScreen(
                                illustsFollowing.itemSnapshotList.items,
                                index,
                                prefix,
                                enableTransition
                            )
                        },
                        onBookmarkClick = { restrict, tags, isEdit ->
                            if (isEdit || !isBookmarked) {
                                BookmarkState.bookmarkIllust(illust.id, restrict, tags)
                            } else {
                                BookmarkState.deleteBookmarkIllust(illust.id)
                            }
                        }
                    )
                }
            }
            VerticalScrollbar(
                state = lazyGridState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
            Row(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalArrangement = 8f.spaceBy
            ) {
                val options = listOf(
                    RStrings.all to Restrict.ALL,
                    RStrings.word_public to Restrict.PUBLIC,
                    RStrings.word_private to Restrict.PRIVATE,
                )
                options.forEach { (label, restrict) ->
                    FilterChip(
                        selected = trendingFilter == restrict,
                        onClick = {
                            viewModel.updateRestrict(restrict)
                            illustsFollowing.refresh()
                        },
                        label = {
                            Text(
                                text = stringResource(label)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendingNovelPage(
    refreshFlow: SharedFlow<LatestPage>,
    modifier: Modifier = Modifier,
    viewModel: LatestViewModel = koinViewModel(),
    navigationManager: NavigationManager = koinInject(),
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

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { novelsFollowing.refresh() },
        modifier = modifier.fillMaxSize(),
        state = pullRefreshState,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    ) {
        Box {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = PaddingValues(vertical = 50.dp),
            ) {
                items(
                    count = novelsFollowing.itemCount,
                    key = novelsFollowing.itemIndexKey { index, item -> "${index}_${item.id}" }
                ) { index ->
                    novelsFollowing[index]?.let { novel ->
                        NovelItem(
                            novel = novel,
                            isBookmarked = novel.isBookmarked,
                            onNovelClick = { novelId ->
                                navigationManager.navigateToNovelDetailScreen(novelId)
                            },
                            onBookmarkClick = { restrict, tags ->
                                if (novel.isBookmarked) {
                                    BookmarkState.deleteBookmarkNovel(novel.id)
                                } else {
                                    BookmarkState.bookmarkNovel(novel.id, restrict, tags)
                                }
                            }
                        )
                    }
                }
            }
            VerticalScrollbar(
                state = lazyListState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
            Row(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalArrangement = 8f.spaceBy
            ) {
                val options = listOf(
                    RStrings.all to Restrict.ALL,
                    RStrings.word_public to Restrict.PUBLIC,
                    RStrings.word_private to Restrict.PRIVATE,
                )
                options.forEach { (label, restrict) ->
                    FilterChip(
                        selected = trendingFilter == restrict,
                        onClick = {
                            viewModel.updateRestrict(restrict)
                            novelsFollowing.refresh()
                        },
                        label = {
                            Text(
                                text = stringResource(label)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    }
}
