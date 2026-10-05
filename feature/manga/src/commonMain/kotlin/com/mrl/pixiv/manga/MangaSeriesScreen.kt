package com.mrl.pixiv.manga

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import com.mrl.pixiv.common.compose.IllustGridDefaults
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.compose.ui.illust.illustGrid
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.load_failed
import com.mrl.pixiv.strings.reading_manga_add_watchlist
import com.mrl.pixiv.strings.reading_manga_chapters_empty
import com.mrl.pixiv.strings.reading_manga_episode_count
import com.mrl.pixiv.strings.reading_manga_remove_watchlist
import com.mrl.pixiv.strings.reading_manga_series
import com.mrl.pixiv.strings.retry
import com.mrl.pixiv.strings.reading_refresh
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MangaSeriesScreen(
    seriesId: Long,
    modifier: Modifier = Modifier,
    viewModel: MangaSeriesViewModel = koinViewModel { parametersOf(seriesId) },
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val updating by viewModel.updating.collectAsStateWithLifecycle()
    val mutationError by viewModel.error.collectAsStateWithLifecycle()
    val works = viewModel.works.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    val layout = IllustGridDefaults.relatedLayoutParameters()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(detail?.title ?: stringResource(RStrings.reading_manga_series)) },
                navigationIcon = {
                    IconButton(onClick = navigationManager::popBackStack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(RStrings.back))
                    }
                },
                actions = {
                    TextButton(onClick = works::refresh) { Text(stringResource(RStrings.reading_refresh)) }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            modifier = Modifier.padding(padding).fillMaxSize(),
            isRefreshing = works.loadState.refresh is LoadState.Loading,
            onRefresh = works::refresh,
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = layout.gridCells,
                horizontalArrangement = layout.horizontalArrangement,
                verticalArrangement = layout.verticalArrangement,
                modifier = Modifier.fillMaxSize(),
            ) {
                detail?.let { series ->
                    item(key = "series_header", span = { GridItemSpan(maxLineSpan) }) {
                        Card(Modifier.fillMaxWidth().padding(12.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(series.title, style = MaterialTheme.typography.titleLarge)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UserAvatar(url = series.user.profileImageUrls.medium, modifier = Modifier.size(36.dp))
                                    TextButton(onClick = { navigationManager.navigateToProfileDetailScreen(series.user.id) }, modifier = Modifier.weight(1f)) {
                                        Text(series.user.name)
                                    }
                                    Text(stringResource(RStrings.reading_manga_episode_count, series.seriesWorkCount))
                                }
                                if (series.caption.isNotBlank()) {
                                    val caption = remember(series.caption) { htmlToAnnotatedString(series.caption, compactMode = true) }
                                    Text(caption, style = MaterialTheme.typography.bodyMedium)
                                }
                                Button(onClick = viewModel::toggleWatchlist, enabled = !updating) {
                                    if (updating) CircularProgressIndicator(Modifier.size(18.dp).padding(end = 4.dp))
                                    Text(stringResource(if (series.watchlistAdded) RStrings.reading_manga_remove_watchlist else RStrings.reading_manga_add_watchlist))
                                }
                                mutationError?.let { Text(stringResource(RStrings.load_failed, it), color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
                if (works.itemCount == 0) {
                    item(key = "refresh_state", span = { GridItemSpan(maxLineSpan) }) {
                        MangaLoadState(works.loadState.refresh, works::retry) {
                            Text(stringResource(RStrings.reading_manga_chapters_empty))
                        }
                    }
                } else if (works.loadState.refresh is LoadState.Error) {
                    item(key = "refresh_error", span = { GridItemSpan(maxLineSpan) }) {
                        MangaLoadState(works.loadState.refresh, works::retry)
                    }
                }
                illustGrid(illusts = works, navToPictureScreen = navigationManager::navigateToPictureScreen)
                item(key = "append_state", span = { GridItemSpan(maxLineSpan) }) {
                    MangaLoadState(works.loadState.append, works::retry)
                }
            }
            VerticalScrollbar(state = gridState, modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
internal fun MangaLoadState(
    state: LoadState,
    onRetry: () -> Unit,
    empty: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state) {
            is LoadState.Loading -> CircularProgressIndicator()
            is LoadState.Error -> {
                Text(stringResource(RStrings.load_failed, state.error.message.orEmpty()))
                Button(onClick = onRetry) { Text(stringResource(RStrings.retry)) }
            }
            is LoadState.NotLoading -> empty()
        }
    }
}
