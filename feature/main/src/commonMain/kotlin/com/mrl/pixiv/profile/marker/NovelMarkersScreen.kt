package com.mrl.pixiv.profile.marker

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.compose.ui.ContentPullToRefreshBox
import com.mrl.pixiv.common.compose.ui.PagingAppendState
import com.mrl.pixiv.common.compose.ui.PagingContentState
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.compose.ui.novel.NovelItem
import com.mrl.pixiv.common.compose.ui.novel.NovelReadLaterButton
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.novel.MarkedNovel
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
        NovelMarkersPage(
            modifier = Modifier.padding(paddingValues),
            viewModel = viewModel,
            navigationManager = navigationManager,
        )
    }
}

@Composable
fun NovelMarkersPage(
    modifier: Modifier = Modifier,
    viewModel: NovelMarkersViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val markers = viewModel.markers.collectAsLazyPagingItems()
    NovelMarkersContent(
        markers = markers,
        modifier = modifier,
        onNovelClick = { novelId, markerPage ->
            navigationManager.navigateToNovelDetailScreen(novelId, markerPage = markerPage)
        },
        onSeriesClick = navigationManager::navigateToNovelSeriesScreen,
        onBookmarkClick = { novel, isBookmarked, restrict, tags ->
            if (isBookmarked) {
                BookmarkState.deleteBookmarkNovel(novel.id)
            } else {
                BookmarkState.bookmarkNovel(novel.id, restrict, tags)
            }
        },
        onDeleteMarker = viewModel::deleteMarker,
    )
}

@Composable
internal fun NovelMarkersContent(
    markers: LazyPagingItems<MarkedNovel>,
    onNovelClick: (Long, Int) -> Unit,
    onSeriesClick: (Long) -> Unit,
    onBookmarkClick: (Novel, Boolean, Restrict, List<String>?) -> Unit,
    onDeleteMarker: (Long) -> Unit,
    modifier: Modifier = Modifier,
    readLaterAction: @Composable (Novel) -> Unit = { NovelReadLaterButton(novel = it) },
) {
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    ContentPullToRefreshBox(
        hasContent = markers.itemCount > 0,
        isRefreshing = markers.loadState.refresh is LoadState.Loading,
        onRefresh = markers::refresh,
        state = pullToRefreshState,
        modifier = modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val compactMarkerLayout = maxWidth < 360.dp ||
                    (maxWidth < 480.dp && LocalDensity.current.fontScale >= 1.3f)
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
            ) {
                items(
                    count = markers.itemCount,
                    key = markers.itemIndexKey { _, item -> item.novel.id },
                ) { index ->
                    markers[index]?.let { markedNovel ->
                        val markerPage = markedNovel.novelMarker?.page?.takeIf { it > 0 }
                            ?: return@let
                        NovelItem(
                            novel = markedNovel.novel,
                            compactMarkerLayout = compactMarkerLayout,
                            onNovelClick = { onNovelClick(it, markerPage) },
                            onSeriesClick = onSeriesClick,
                            onBookmarkClick = { isBookmarked, restrict, tags ->
                                onBookmarkClick(markedNovel.novel, isBookmarked, restrict, tags)
                            },
                            markerPageLabel = stringResource(
                                RStrings.novel_marker_page,
                                markerPage
                            ),
                            onMarkerClick = { onDeleteMarker(markedNovel.novel.id) },
                            readLaterAction = { readLaterAction(markedNovel.novel) },
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
