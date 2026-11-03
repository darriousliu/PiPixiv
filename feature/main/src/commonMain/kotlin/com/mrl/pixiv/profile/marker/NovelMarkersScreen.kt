package com.mrl.pixiv.profile.marker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.compose.ui.ContentPullToRefreshBox
import com.mrl.pixiv.common.compose.ui.PagingAppendState
import com.mrl.pixiv.common.compose.ui.PagingContentState
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.compose.ui.novel.NovelItem
import com.mrl.pixiv.common.kts.itemIndexKey
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.novel_marker_page
import com.mrl.pixiv.strings.novel_markers
import com.mrl.pixiv.strings.novel_markers_empty
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NovelMarkersScreen(
    modifier: Modifier = Modifier,
    viewModel: NovelMarkersViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val markers = viewModel.markers.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    val refreshState = markers.loadState.refresh
    val isRefreshing = refreshState is LoadState.Loading

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(RStrings.novel_markers)) },
                navigationIcon = {
                    IconButton(onClick = navigationManager::popBackStack) {
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
            hasContent = markers.itemCount > 0,
            isRefreshing = isRefreshing,
            onRefresh = markers::refresh,
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(
                        count = markers.itemCount,
                        key = markers.itemIndexKey { _, item -> item.novel.id },
                    ) { index ->
                        markers[index]?.let { markedNovel ->
                            val markerPage = markedNovel.novelMarker
                                ?.page
                                ?.takeIf { it > 0 }
                                ?: return@let
                            NovelItem(
                                novel = markedNovel.novel,
                                onNovelClick = { novelId ->
                                    navigationManager.navigateToNovelDetailScreen(
                                        novelId = novelId,
                                        markerPage = markerPage,
                                    )
                                },
                                onSeriesClick = navigationManager::navigateToNovelSeriesScreen,
                                onBookmarkClick = { isBookmarked, restrict, tags ->
                                    if (isBookmarked) {
                                        BookmarkState.deleteBookmarkNovel(
                                            markedNovel.novel.id
                                        )
                                    } else {
                                        BookmarkState.bookmarkNovel(
                                            markedNovel.novel.id,
                                            restrict,
                                            tags,
                                        )
                                    }
                                },
                                markerPageLabel = stringResource(
                                    RStrings.novel_marker_page,
                                    markerPage,
                                ),
                                onMarkerClick = {
                                    viewModel.deleteMarker(markedNovel.novel.id)
                                },
                            )
                        }
                    }
                    if (
                        markers.itemCount > 0 &&
                        markers.loadState.refresh is LoadState.NotLoading &&
                        markers.loadState.append !is LoadState.NotLoading
                    ) {
                        item(key = "append_state") {
                            PagingAppendState(
                                loadStates = markers.loadState,
                                itemCount = markers.itemCount,
                                onRetry = markers::retry,
                            )
                        }
                    }
                }
                PagingContentState(
                    loadStates = markers.loadState,
                    itemCount = markers.itemCount,
                    onRetry = markers::retry,
                    emptyMessage = stringResource(RStrings.novel_markers_empty),
                )
                VerticalScrollbar(
                    state = listState,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}
