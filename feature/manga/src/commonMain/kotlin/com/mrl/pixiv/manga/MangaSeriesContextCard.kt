package com.mrl.pixiv.manga

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    viewModel: MangaContextViewModel = koinViewModel(key = "manga_context_${illust.id}") { parametersOf(illust.id) },
) {
    val series = illust.series ?: return
    val seriesId = series.id?.takeIf { it > 0 } ?: return
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = state.response?.context
    val previous = context?.prev?.let { listOf(it).visibleMangaWorks().firstOrNull() }
    val next = context?.next?.let { listOf(it).visibleMangaWorks().firstOrNull() }
    Card(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(state.response?.detail?.title ?: series.title.orEmpty(), style = MaterialTheme.typography.titleMedium)
            context?.contentOrder?.takeIf { it > 0 }?.let { Text(stringResource(RStrings.reading_manga_episode_order, it)) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onSeriesClick(seriesId) }) {
                    Text(stringResource(RStrings.reading_manga_view_directory))
                }
                previous?.let { chapter ->
                    TextButton(onClick = { onEpisodeClick(chapter.id) }) { Text(stringResource(RStrings.reading_manga_previous)) }
                }
                next?.let { chapter ->
                    TextButton(onClick = { onEpisodeClick(chapter.id) }) { Text(stringResource(RStrings.reading_manga_next)) }
                }
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let {
                Text(stringResource(RStrings.load_failed, it), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::load) { Text(stringResource(RStrings.retry)) }
            }
        }
    }
}
