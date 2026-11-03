package com.mrl.pixiv.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.mrl.pixiv.common.compose.ui.ContentPullToRefreshBox
import com.mrl.pixiv.common.compose.ui.PagingContentState
import com.mrl.pixiv.common.compose.ui.PagingAppendState
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.data.manga.MangaSeriesDetail
import com.mrl.pixiv.common.data.manga.MangaWatchlistEntry
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.convertUtcStringToLocalDateTime
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.reading_manga_empty
import com.mrl.pixiv.strings.reading_manga_episode_count
import com.mrl.pixiv.strings.reading_manga_latest
import com.mrl.pixiv.strings.reading_manga_user_series
import com.mrl.pixiv.strings.reading_manga_watchlist
import com.mrl.pixiv.strings.reading_manga_watchlist_empty
import com.mrl.pixiv.strings.reading_refresh
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MangaWatchlistScreen(
    modifier: Modifier = Modifier,
    viewModel: MangaWatchlistViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val series = viewModel.series.collectAsLazyPagingItems()
    MangaListScaffold(
        title = stringResource(RStrings.reading_manga_watchlist),
        modifier = modifier,
        onBack = navigationManager::popBackStack,
        onRefresh = series::refresh,
    ) { contentModifier ->
        MangaWatchlistContent(
            watchlist = series,
            modifier = contentModifier,
            navigationManager = navigationManager,
        )
    }
}

/** Shared list body for the Latest tab and the independent watchlist screen. */
@Composable
fun MangaWatchlistContent(
    watchlist: LazyPagingItems<MangaWatchlistEntry>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    MangaPagedListContent(
        emptyText = stringResource(RStrings.reading_manga_watchlist_empty),
        items = watchlist,
        modifier = modifier,
        listState = listState,
    ) { entry ->
        MangaWatchlistCard(
            entry = entry,
            onSeriesClick = navigationManager::navigateToMangaSeriesScreen,
            onLatestClick = navigationManager::navigateToSinglePictureScreen,
        )
    }
}

@Composable
fun UserMangaSeriesScreen(
    userId: Long,
    modifier: Modifier = Modifier,
    viewModel: UserMangaSeriesViewModel = koinViewModel { parametersOf(userId) },
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val series = viewModel.series.collectAsLazyPagingItems()
    MangaListScaffold(
        title = stringResource(RStrings.reading_manga_user_series),
        modifier = modifier,
        onBack = navigationManager::popBackStack,
        onRefresh = series::refresh,
    ) { contentModifier ->
        MangaPagedListContent(
            emptyText = stringResource(RStrings.reading_manga_empty),
            items = series,
            modifier = contentModifier,
        ) { entry ->
            MangaSeriesCard(entry) { navigationManager.navigateToMangaSeriesScreen(entry.id) }
        }
    }
}

@Composable
internal fun MangaListScaffold(
    title: String,
    modifier: Modifier,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(RStrings.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, stringResource(RStrings.reading_refresh))
                    }
                },
            )
        },
    ) { padding ->
        content(Modifier.padding(padding))
    }
}

@Composable
private fun <T : Any> MangaPagedListContent(
    emptyText: String,
    items: LazyPagingItems<T>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    content: @Composable (T) -> Unit,
) {
    val isRefreshing = items.loadState.refresh is LoadState.Loading
    ContentPullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        isRefreshing = isRefreshing,
        hasContent = items.itemCount > 0,
        onRefresh = items::refresh,
    ) {
        MangaListColumn(state = listState) {
            if (items.itemCount == 0) {
                item(key = "refresh_state") {
                    PagingContentState(
                        loadStates = items.loadState,
                        itemCount = items.itemCount,
                        onRetry = items::retry,
                        modifier = Modifier.fillParentMaxHeight(),
                        emptyMessage = emptyText,
                    )
                }
            } else if (items.loadState.refresh is LoadState.Error) {
                item(key = "refresh_error") {
                    PagingContentState(items.loadState, items.itemCount, items::retry)
                }
            }
            // Masked entries have no stable server ID, so use the paging position.
            items(count = items.itemCount) { index -> items[index]?.let { content(it) } }
            if (items.itemCount > 0 &&
                items.loadState.refresh is LoadState.NotLoading &&
                items.loadState.append !is LoadState.NotLoading
            ) {
                item(key = "append_state") {
                    PagingAppendState(
                        items.loadState,
                        items.itemCount,
                        items::retry
                    )
                }
            }
        }
    }
}

/** A comfortable reading width shared by full-screen lists, embedded lists and previews. */
@Composable
internal fun MangaListColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = state,
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                content = content,
            )
            VerticalScrollbar(state = state, modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
internal fun MangaWatchlistCard(
    entry: MangaWatchlistEntry,
    onSeriesClick: (Long) -> Unit,
    onLatestClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = { entry.navigableSeriesId?.let(onSeriesClick) },
        enabled = entry.navigableSeriesId != null,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        if (entry.isMasked) {
            Row(
                Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.VisibilityOff,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    entry.maskText.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MangaCover(entry.url, Modifier.width(88.dp).height(124.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        entry.title.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    entry.user?.let { MangaAuthorLabel(it.name) }
                    entry.publishedContentCount?.let { MangaEpisodeCount(it) }
                    entry.lastPublishedContentDatetime?.takeIf { it.isNotBlank() }?.let { date ->
                        Text(
                            remember(date) {
                                runCatching { convertUtcStringToLocalDateTime(date) }.getOrDefault(
                                    date
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            entry.navigableLatestId?.let { id ->
                FilledTonalButton(
                    onClick = { onLatestClick(id) },
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        stringResource(RStrings.reading_manga_latest),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
internal fun MangaSeriesCard(
    entry: MangaSeriesDetail,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MangaCover(entry.coverImageUrls.medium, Modifier.width(80.dp).height(112.dp))
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                MangaAuthorLabel(entry.user.name)
                MangaEpisodeCount(entry.seriesWorkCount)
            }
        }
    }
}

@Composable
internal fun MangaCover(url: String?, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.MenuBook,
            null,
            Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        )
        if (!url.isNullOrBlank()) {
            AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun MangaAuthorLabel(name: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Rounded.Person,
            null,
            Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun MangaEpisodeCount(count: Int) {
    Text(
        stringResource(RStrings.reading_manga_episode_count, count),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}
