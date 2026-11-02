package com.mrl.pixiv.manga

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import com.mrl.pixiv.common.compose.IllustGridDefaults
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.data.manga.MangaSeriesDetail
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.load_failed
import com.mrl.pixiv.strings.reading_manga_add_watchlist
import com.mrl.pixiv.strings.reading_manga_chapters_empty
import com.mrl.pixiv.strings.reading_manga_remove_watchlist
import com.mrl.pixiv.strings.reading_manga_series
import com.mrl.pixiv.strings.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

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
    MangaListScaffold(
        title = detail?.title ?: stringResource(RStrings.reading_manga_series),
        modifier = modifier,
        onBack = navigationManager::popBackStack,
        onRefresh = works::refresh,
    ) { contentModifier ->
        PullToRefreshBox(
            modifier = contentModifier.fillMaxSize(),
            isRefreshing = works.loadState.refresh is LoadState.Loading,
            onRefresh = works::refresh,
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = layout.gridCells,
                horizontalArrangement = layout.horizontalArrangement,
                verticalArrangement = layout.verticalArrangement,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            ) {
                detail?.let { series ->
                    item(key = "series_header", span = { GridItemSpan(maxLineSpan) }) {
                        MangaSeriesHeader(
                            series = series,
                            updating = updating,
                            error = mutationError,
                            onAuthorClick = { navigationManager.navigateToProfileDetailScreen(series.user.id) },
                            onToggleWatchlist = viewModel::toggleWatchlist,
                            modifier = Modifier.padding(bottom = 12.dp),
                            inset = false,
                        )
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
                items(
                    works.itemCount,
                    key = { index -> "chapter_${index}_${works.peek(index)?.id}" }) { index ->
                    works[index]?.let { illust ->
                        MangaChapter(illust) {
                            navigationManager.navigateToPictureScreen(
                                works.itemSnapshotList.items,
                                index,
                                "manga_${Uuid.random().toHexString()}",
                                false,
                            )
                        }
                    }
                }
                item(key = "append_state", span = { GridItemSpan(maxLineSpan) }) {
                    MangaLoadState(works.loadState.append, works::retry)
                }
            }
            VerticalScrollbar(state = gridState, modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
internal fun MangaSeriesHeader(
    series: MangaSeriesDetail,
    updating: Boolean,
    error: String?,
    onAuthorClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    modifier: Modifier = Modifier,
    inset: Boolean = true,
) {
    Card(
        modifier = modifier.fillMaxWidth().then(if (inset) Modifier.padding(12.dp) else Modifier),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MangaCover(series.coverImageUrls.medium, Modifier.width(96.dp).height(136.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        series.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier.clip(MaterialTheme.shapes.small)
                            .clickable(onClick = onAuthorClick)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        UserAvatar(
                            series.user.profileImageUrls.medium,
                            modifier = Modifier.size(28.dp),
                            onClick = onAuthorClick
                        )
                        Text(
                            series.user.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    MangaEpisodeCount(series.seriesWorkCount)
                }
            }
            if (series.caption.isNotBlank()) {
                val caption = remember(series.caption) {
                    htmlToAnnotatedString(
                        series.caption,
                        compactMode = true
                    )
                }
                Text(
                    caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onToggleWatchlist,
                enabled = !updating,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                colors = if (series.watchlistAdded) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.buttonColors(),
            ) {
                if (updating) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        if (series.watchlistAdded) Icons.Rounded.Check else Icons.Rounded.Add,
                        null,
                        Modifier.size(18.dp)
                    )
                }
                Text(
                    stringResource(if (series.watchlistAdded) RStrings.reading_manga_remove_watchlist else RStrings.reading_manga_add_watchlist),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            error?.let {
                Text(
                    stringResource(RStrings.load_failed, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
internal fun MangaLoadState(
    state: LoadState,
    onRetry: () -> Unit,
    empty: (@Composable () -> Unit)? = null,
) {
    if (state is LoadState.NotLoading && empty == null) return
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (state) {
            is LoadState.Loading -> CircularProgressIndicator(
                Modifier.size(32.dp),
                strokeWidth = 3.dp
            )

            is LoadState.Error -> {
                Icon(
                    Icons.Rounded.CloudOff,
                    null,
                    Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(RStrings.load_failed, state.error.message.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = onRetry) { Text(stringResource(RStrings.retry)) }
            }

            is LoadState.NotLoading -> {
                Icon(
                    Icons.AutoMirrored.Rounded.MenuBook,
                    null,
                    Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ProvideTextStyle(
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                ) { empty?.invoke() }
            }
        }
    }
}
