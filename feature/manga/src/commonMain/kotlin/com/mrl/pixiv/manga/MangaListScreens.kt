package com.mrl.pixiv.manga

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.data.manga.MangaSeriesDetail
import com.mrl.pixiv.common.data.manga.MangaWatchlistEntry
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
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
    MangaPagedList(
        title = stringResource(RStrings.reading_manga_watchlist),
        emptyText = stringResource(RStrings.reading_manga_watchlist_empty),
        items = series,
        modifier = modifier,
        onBack = navigationManager::popBackStack,
    ) { entry ->
        MangaWatchlistCard(entry, navigationManager)
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
    MangaPagedList(
        title = stringResource(RStrings.reading_manga_user_series),
        emptyText = stringResource(RStrings.reading_manga_empty),
        items = series,
        modifier = modifier,
        onBack = navigationManager::popBackStack,
    ) { entry ->
        MangaSeriesCard(entry) { navigationManager.navigateToMangaSeriesScreen(entry.id) }
    }
}

@Composable
private fun <T : Any> MangaPagedList(
    title: String,
    emptyText: String,
    items: LazyPagingItems<T>,
    modifier: Modifier,
    onBack: () -> Unit,
    content: @Composable (T) -> Unit,
) {
    val listState = rememberLazyListState()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(RStrings.back)) }
                },
                actions = {
                    TextButton(onClick = items::refresh) { Text(stringResource(RStrings.reading_refresh)) }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            modifier = Modifier.padding(padding).fillMaxSize(),
            isRefreshing = items.loadState.refresh is LoadState.Loading,
            onRefresh = items::refresh,
        ) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                if (items.itemCount == 0) {
                    item(key = "refresh_state") {
                        MangaLoadState(items.loadState.refresh, items::retry) { Text(emptyText) }
                    }
                } else if (items.loadState.refresh is LoadState.Error) {
                    item(key = "refresh_error") { MangaLoadState(items.loadState.refresh, items::retry) }
                }
                // Masked entries have no stable server ID, so use the paging position.
                items(count = items.itemCount) { index -> items[index]?.let { content(it) } }
                item(key = "append_state") { MangaLoadState(items.loadState.append, items::retry) }
            }
            VerticalScrollbar(state = listState, modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun MangaWatchlistCard(entry: MangaWatchlistEntry, navigationManager: NavigationManager) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(enabled = entry.navigableSeriesId != null) {
                entry.navigableSeriesId?.let(navigationManager::navigateToMangaSeriesScreen)
            },
    ) {
        if (entry.isMasked) {
            Text(entry.maskText.orEmpty(), Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!entry.url.isNullOrBlank()) {
                    AsyncImage(entry.url, entry.title, modifier = Modifier.size(84.dp), contentScale = ContentScale.Crop)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(entry.title.orEmpty(), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    entry.user?.let { Text(it.name, style = MaterialTheme.typography.bodyMedium) }
                    entry.publishedContentCount?.let { Text(stringResource(RStrings.reading_manga_episode_count, it)) }
                    entry.lastPublishedContentDatetime?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    entry.navigableLatestId?.let { id ->
                        TextButton(onClick = { navigationManager.navigateToSinglePictureScreen(id) }) {
                            Text(stringResource(RStrings.reading_manga_latest))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaSeriesCard(entry: MangaSeriesDetail, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (entry.coverImageUrls.medium.isNotBlank()) {
                AsyncImage(entry.coverImageUrls.medium, entry.title, modifier = Modifier.size(84.dp), contentScale = ContentScale.Crop)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium)
                Text(entry.user.name)
                Text(stringResource(RStrings.reading_manga_episode_count, entry.seriesWorkCount))
            }
        }
    }
}
