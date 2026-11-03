package com.mrl.pixiv.latest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.mrl.pixiv.common.compose.layout.LocalMainNavigationBottomInset
import com.mrl.pixiv.common.compose.layout.withMainNavigationBottomInset
import com.mrl.pixiv.common.compose.listener.KeyEventListener
import com.mrl.pixiv.common.compose.listener.keyboardScrollerController
import com.mrl.pixiv.common.compose.ui.ContentPullToRefreshBox
import com.mrl.pixiv.common.compose.ui.PagingAppendState
import com.mrl.pixiv.common.compose.ui.PagingContentState
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.compose.ui.novel.NovelItem
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.novel.NovelWatchlistSeries
import com.mrl.pixiv.common.kts.itemIndexKey
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.novel_series_chapter_count
import com.mrl.pixiv.strings.novel_watchlist_empty
import com.mrl.pixiv.strings.switch_to_latest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NewNovelPage(
    refreshFlow: SharedFlow<LatestPage>,
    modifier: Modifier = Modifier,
    viewModel: LatestViewModel = koinViewModel(),
) {
    NovelFeedPage(
        page = LatestPage.NovelNew,
        novels = viewModel.newNovels,
        listState = viewModel.newNovelLazyListState,
        refreshFlow = refreshFlow,
        modifier = modifier,
    )
}

@Composable
private fun NovelFeedPage(
    page: LatestPage,
    novels: Flow<PagingData<Novel>>,
    listState: LazyListState,
    refreshFlow: SharedFlow<LatestPage>,
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val pagingItems = novels.collectAsLazyPagingItems()
    val pullRefreshState = rememberPullToRefreshState()
    val isRefreshing = pagingItems.loadState.refresh is LoadState.Loading
    val controller = remember(listState) {
        keyboardScrollerController(listState) {
            listState.layoutInfo.viewportSize.height.toFloat()
        }
    }

    KeyEventListener(controller)

    LaunchedEffect(refreshFlow, page) {
        refreshFlow.collect { refreshedPage ->
            if (refreshedPage == page) {
                pagingItems.refresh()
            }
        }
    }

    ContentPullToRefreshBox(
        hasContent = pagingItems.itemCount > 0,
        isRefreshing = isRefreshing,
        onRefresh = pagingItems::refresh,
        modifier = modifier.fillMaxSize(),
        state = pullRefreshState,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp).withMainNavigationBottomInset(),
            ) {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.id },
                ) { index ->
                    val novel = pagingItems[index] ?: return@items
                    NovelItem(
                        novel = novel,
                        onNovelClick = navigationManager::navigateToNovelDetailScreen,
                        onSeriesClick = navigationManager::navigateToNovelSeriesScreen,
                        onBookmarkClick = { isBookmarked, restrict, tags ->
                            if (isBookmarked) {
                                BookmarkState.deleteBookmarkNovel(novel.id)
                            } else {
                                BookmarkState.bookmarkNovel(novel.id, restrict, tags)
                            }
                        },
                    )
                }

                if (
                    pagingItems.itemCount > 0 &&
                    pagingItems.loadState.refresh is LoadState.NotLoading &&
                    pagingItems.loadState.append !is LoadState.NotLoading
                ) {
                    item(key = "append_state") {
                        PagingAppendState(
                            loadStates = pagingItems.loadState,
                            itemCount = pagingItems.itemCount,
                            onRetry = pagingItems::retry,
                            modifier = Modifier.padding(bottom = 136.dp),
                        )
                    }
                }
            }
            PagingContentState(
                loadStates = pagingItems.loadState,
                itemCount = pagingItems.itemCount,
                onRetry = pagingItems::retry,
                modifier = Modifier.padding(
                    bottom = LocalMainNavigationBottomInset.current +
                            if (pagingItems.itemCount > 0) 136.dp else 0.dp,
                ),
            )
            VerticalScrollbar(
                state = listState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
fun NovelWatchlistPage(
    refreshFlow: SharedFlow<LatestPage>,
    modifier: Modifier = Modifier,
    viewModel: LatestViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val watchlist = viewModel.novelWatchlist.collectAsLazyPagingItems()
    val listState = viewModel.watchlistNovelLazyListState
    val pullRefreshState = rememberPullToRefreshState()
    val isRefreshing = watchlist.loadState.refresh is LoadState.Loading
    val controller = remember(listState) {
        keyboardScrollerController(listState) {
            listState.layoutInfo.viewportSize.height.toFloat()
        }
    }

    KeyEventListener(controller)

    LaunchedEffect(refreshFlow) {
        refreshFlow.collect { page ->
            if (page == LatestPage.NovelWatchlist) {
                watchlist.refresh()
            }
        }
    }

    ContentPullToRefreshBox(
        hasContent = watchlist.itemCount > 0,
        isRefreshing = isRefreshing,
        onRefresh = watchlist::refresh,
        modifier = modifier.fillMaxSize(),
        state = pullRefreshState,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp).withMainNavigationBottomInset(),
            ) {
                items(
                    count = watchlist.itemCount,
                    key = watchlist.itemIndexKey { index, item ->
                        item.id ?: "masked_${index}_${item.maskText.orEmpty()}"
                    },
                ) { index ->
                    val series = watchlist[index] ?: return@items
                    NovelWatchlistItem(
                        series = series,
                        onSeriesClick = navigationManager::navigateToNovelSeriesScreen,
                        onLatestNovelClick = navigationManager::navigateToNovelDetailScreen,
                    )
                }

                if (
                    watchlist.itemCount > 0 &&
                    watchlist.loadState.refresh is LoadState.NotLoading &&
                    watchlist.loadState.append !is LoadState.NotLoading
                ) {
                    item(key = "append_state") {
                        PagingAppendState(
                            loadStates = watchlist.loadState,
                            itemCount = watchlist.itemCount,
                            onRetry = watchlist::retry,
                            modifier = Modifier.padding(bottom = 136.dp),
                        )
                    }
                }
            }
            PagingContentState(
                loadStates = watchlist.loadState,
                itemCount = watchlist.itemCount,
                onRetry = watchlist::retry,
                modifier = Modifier.padding(
                    bottom = LocalMainNavigationBottomInset.current +
                            if (watchlist.itemCount > 0) 136.dp else 0.dp,
                ),
                emptyMessage = stringResource(RStrings.novel_watchlist_empty),
            )
            VerticalScrollbar(
                state = listState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun NovelWatchlistItem(
    series: NovelWatchlistSeries,
    onSeriesClick: (Long) -> Unit,
    onLatestNovelClick: (Long) -> Unit,
) {
    val seriesId = series.id?.takeIf { it > 0L }
    val canOpenSeries = seriesId != null && !series.isMasked

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(enabled = canOpenSeries) {
                seriesId?.let(onSeriesClick)
            },
    ) {
        if (series.isMasked) {
            Text(
                text = series.maskText.orEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Card
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .heightIn(min = 120.dp),
        ) {
            if (series.url.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            } else {
                AsyncImage(
                    model = series.url,
                    contentDescription = series.title,
                    modifier = Modifier
                        .width(84.dp)
                        .fillMaxHeight(),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = series.title.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                series.user?.let { user ->
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            RStrings.novel_series_chapter_count,
                            series.publishedContentCount ?: 0,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    series.lastPublishedContentDatetime
                        ?.takeIf(String::isNotBlank)
                        ?.take(10)
                        ?.let { date ->
                            Text(
                                text = date,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                }
                series.latestContentId?.takeIf { it > 0L }?.let { novelId ->
                    TextButton(
                        onClick = { onLatestNovelClick(novelId) },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(text = stringResource(RStrings.switch_to_latest))
                    }
                }
            }
        }
    }
}
