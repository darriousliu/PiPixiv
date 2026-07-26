package com.mrl.pixiv.novel.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
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
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.compose.ui.novel.NovelItem
import com.mrl.pixiv.common.kts.HSpacer
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.load_failed
import com.mrl.pixiv.strings.novel_series_chapter_count
import com.mrl.pixiv.strings.novel_watchlist_add
import com.mrl.pixiv.strings.novel_watchlist_added
import com.mrl.pixiv.strings.retry
import com.mrl.pixiv.strings.series
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun NovelSeriesScreen(
    seriesId: Long,
    modifier: Modifier = Modifier,
    viewModel: NovelSeriesViewModel = koinViewModel { parametersOf(seriesId) },
    navigationManager: NavigationManager = koinInject(),
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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = novels::refresh,
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
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

                    if (
                        novels.itemCount == 0 &&
                        novels.loadState.refresh is LoadState.Loading
                    ) {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularWavyProgressIndicator()
                            }
                        }
                    }

                    val refreshError = novels.loadState.refresh as? LoadState.Error
                    if (novels.itemCount == 0 && refreshError != null) {
                        item(key = "refresh_error") {
                            SeriesLoadError(
                                message = refreshError.error.message.orEmpty(),
                                onRetry = novels::retry,
                            )
                        }
                    }

                    items(
                        count = novels.itemCount,
                        key = novels.itemKey { it.id },
                    ) { index ->
                        val novel = novels[index] ?: return@items
                        NovelItem(
                            novel = novel,
                            onNovelClick = navigationManager::navigateToNovelDetailScreen,
                            onBookmarkClick = { isBookmarked, restrict, tags ->
                                if (isBookmarked) {
                                    BookmarkState.deleteBookmarkNovel(novel.id)
                                } else {
                                    BookmarkState.bookmarkNovel(novel.id, restrict, tags)
                                }
                            },
                        )
                    }

                    when (val appendState = novels.loadState.append) {
                        is LoadState.Loading -> item(key = "append_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is LoadState.Error -> item(key = "append_error") {
                            SeriesLoadError(
                                message = appendState.error.message.orEmpty(),
                                onRetry = novels::retry,
                            )
                        }

                        else -> Unit
                    }
                }
                VerticalScrollbar(
                    state = listState,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}

@Composable
private fun SeriesLoadError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(RStrings.load_failed, message),
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) {
            Text(text = stringResource(RStrings.retry))
        }
    }
}
