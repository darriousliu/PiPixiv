package com.mrl.pixiv.novel.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.mrl.pixiv.common.compose.ui.ContentPullToRefreshBox
import com.mrl.pixiv.common.compose.ui.PagingAppendState
import com.mrl.pixiv.common.compose.ui.PagingContentState
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.compose.ui.novel.NovelItem
import com.mrl.pixiv.common.kts.HSpacer
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.novel_series_chapter_count
import com.mrl.pixiv.strings.novel_series_last_read
import com.mrl.pixiv.strings.novel_watchlist_add
import com.mrl.pixiv.strings.novel_watchlist_added
import com.mrl.pixiv.strings.series
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun NovelSeriesScreen(
    seriesId: Long,
    modifier: Modifier = Modifier,
    viewModel: NovelSeriesViewModel = koinViewModel { parametersOf(seriesId) },
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val novels = viewModel.novels.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()
    val isRefreshing = novels.loadState.refresh is LoadState.Loading

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.detail?.title ?: stringResource(RStrings.series),
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigationManager::popBackStack,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(RStrings.back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        ContentPullToRefreshBox(
            isRefreshing = isRefreshing,
            hasContent = novels.itemCount > 0,
            onRefresh = novels::refresh,
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    state.detail?.let { detail ->
                        item(key = "series_header") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        text = detail.title,
                                        style = MaterialTheme.typography.headlineSmall,
                                    )
                                    if (detail.caption.isNotBlank()) {
                                        Text(
                                            text = detail.caption,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        UserAvatar(
                                            url = detail.user.profileImageUrls.medium,
                                            modifier = Modifier.size(40.dp),
                                            onClick = {
                                                navigationManager.navigateToProfileDetailScreen(
                                                    detail.user.id,
                                                )
                                            },
                                        )
                                        8.HSpacer
                                        TextButton(
                                            onClick = {
                                                navigationManager.navigateToProfileDetailScreen(
                                                    detail.user.id,
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                text = detail.user.name,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Start,
                                            )
                                        }
                                        Text(
                                            text = stringResource(
                                                RStrings.novel_series_chapter_count,
                                                detail.contentCount,
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    }
                                    Button(
                                        onClick = viewModel::toggleWatchlist,
                                        enabled = !state.isUpdatingWatchlist,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        if (state.isUpdatingWatchlist) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                            )
                                        } else {
                                            Text(
                                                text = stringResource(
                                                    if (detail.watchlistAdded) {
                                                        RStrings.novel_watchlist_added
                                                    } else {
                                                        RStrings.novel_watchlist_add
                                                    },
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    state.lastRead?.let { progress ->
                        item(key = "continue_reading") {
                            NovelSeriesContinueReadingCard(
                                progress = progress,
                                onClick = { navigationManager.navigateToNovelDetailScreen(progress.novelId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }

                    if (novels.itemCount == 0) {
                        item(key = "content_state") {
                            PagingContentState(
                                loadStates = novels.loadState,
                                itemCount = novels.itemCount,
                                onRetry = novels::retry,
                                modifier = if (state.detail == null && state.lastRead == null) {
                                    Modifier.fillParentMaxHeight()
                                } else {
                                    Modifier.fillMaxWidth().heightIn(min = 240.dp)
                                },
                            )
                        }
                    }

                    items(
                        count = novels.itemCount,
                        key = novels.itemKey { it.id },
                    ) { index ->
                        val novel = novels[index] ?: return@items
                        if (state.lastRead?.novelId == novel.id) {
                            Text(
                                stringResource(RStrings.novel_series_last_read),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
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
                        novels.itemCount > 0 &&
                        novels.loadState.refresh is LoadState.NotLoading &&
                        novels.loadState.append !is LoadState.NotLoading
                    ) {
                        item(key = "append_state") {
                            PagingAppendState(novels.loadState, novels.itemCount, novels::retry)
                        }
                    }
                }
                if (novels.itemCount > 0) {
                    PagingContentState(
                        loadStates = novels.loadState,
                        itemCount = novels.itemCount,
                        onRetry = novels::retry,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                VerticalScrollbar(
                    state = listState,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}
