package com.mrl.pixiv.manga

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.repository.paging.visibleMangaWorks
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.load_failed
import com.mrl.pixiv.strings.reading_manga_episode_order
import com.mrl.pixiv.strings.reading_manga_next
import com.mrl.pixiv.strings.reading_manga_previous
import com.mrl.pixiv.strings.reading_manga_view_directory
import com.mrl.pixiv.strings.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Adds series navigation to the existing picture details; chapters still open the picture screen. */
@Composable
fun MangaSeriesContextCard(
    illust: Illust,
    onSeriesClick: (Long) -> Unit,
    onEpisodeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MangaContextViewModel = koinViewModel(key = "manga_context_${illust.id}") {
        parametersOf(
            illust.id
        )
    },
) {
    val series = illust.series ?: return
    val seriesId = series.id?.takeIf { it > 0 } ?: return
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = state.response?.context
    val previous = context?.prev?.let { listOf(it).visibleMangaWorks().firstOrNull() }
    val next = context?.next?.let { listOf(it).visibleMangaWorks().firstOrNull() }
    MangaSeriesContextContent(
        title = state.response?.detail?.title ?: series.title.orEmpty(),
        contentOrder = context?.contentOrder,
        previousId = previous?.id,
        nextId = next?.id,
        loading = state.loading,
        error = state.error,
        onSeriesClick = { onSeriesClick(seriesId) },
        onEpisodeClick = onEpisodeClick,
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

@Composable
internal fun MangaSeriesContextContent(
    title: String,
    contentOrder: Int?,
    previousId: Long?,
    nextId: Long?,
    loading: Boolean,
    error: String?,
    onSeriesClick: () -> Unit,
    onEpisodeClick: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.MenuBook,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    contentOrder?.takeIf { it > 0 }?.let {
                        Text(
                            stringResource(RStrings.reading_manga_episode_order, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            TextButton(
                onClick = onSeriesClick,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(stringResource(RStrings.reading_manga_view_directory), Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp))
            }
            if (previousId != null || nextId != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { previousId?.let(onEpisodeClick) },
                        enabled = previousId != null,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(18.dp))
                        Text(
                            stringResource(RStrings.reading_manga_previous),
                            Modifier.padding(start = 6.dp)
                        )
                    }
                    FilledTonalButton(
                        onClick = { nextId?.let(onEpisodeClick) },
                        enabled = nextId != null,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(
                            stringResource(RStrings.reading_manga_next),
                            Modifier.padding(end = 6.dp)
                        )
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp))
                    }
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let {
                Text(
                    stringResource(RStrings.load_failed, it),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onRetry) { Text(stringResource(RStrings.retry)) }
            }
        }
    }
}
