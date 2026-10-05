package com.mrl.pixiv.novel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.mrl.pixiv.common.data.novel.PublicNovelPreview
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.load_failed
import com.mrl.pixiv.strings.reading_novel_interactions
import com.mrl.pixiv.strings.reading_novel_recommendations
import com.mrl.pixiv.strings.reading_novel_public_note
import com.mrl.pixiv.strings.reading_novel_poll
import com.mrl.pixiv.strings.reading_novel_poll_absent
import com.mrl.pixiv.strings.reading_novel_poll_vote
import com.mrl.pixiv.strings.reading_novel_poll_voted
import com.mrl.pixiv.strings.reading_novel_poll_count
import com.mrl.pixiv.strings.reading_novel_recommendations_empty
import com.mrl.pixiv.strings.reading_refresh
import com.mrl.pixiv.strings.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun NovelInteractionsBottomSheet(
    novelId: Long,
    onDismissRequest: () -> Unit,
    onNovelClick: (Long) -> Unit,
    viewModel: NovelInteractionsViewModel = koinViewModel(key = "novel_interactions_$novelId") { parametersOf(novelId) },
) {
    val recommendations = viewModel.recommendations.collectAsLazyPagingItems()
    val poll by viewModel.poll.collectAsStateWithLifecycle()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
            item(key = "heading") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(RStrings.reading_novel_interactions), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(RStrings.reading_novel_public_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item(key = "poll") {
                NovelPollCard(poll, viewModel::selectChoice, viewModel::answerPoll, viewModel::loadPoll)
            }
            item(key = "recommendations_heading") {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(RStrings.reading_novel_recommendations), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = recommendations::refresh) { Text(stringResource(RStrings.reading_refresh)) }
                }
            }
            if (recommendations.itemCount == 0) {
                item(key = "initial_state") {
                    NovelRecommendationLoadState(recommendations.loadState.refresh, recommendations::retry, showEmpty = true)
                }
            } else if (recommendations.loadState.refresh is LoadState.Error) {
                item(key = "refresh_error") { NovelRecommendationLoadState(recommendations.loadState.refresh, recommendations::retry) }
            }
            items(recommendations.itemCount) { index ->
                recommendations[index]?.let { preview -> PublicNovelCard(preview, onNovelClick) }
            }
            item(key = "append_state") {
                NovelRecommendationLoadState(recommendations.loadState.append, recommendations::retry)
            }
        }
    }
}

@Composable
private fun NovelPollCard(state: NovelPollState, onSelect: (Int) -> Unit, onAnswer: () -> Unit, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(RStrings.reading_novel_poll), style = MaterialTheme.typography.titleMedium)
        if (state.loading) CircularProgressIndicator(Modifier.size(24.dp))
        state.error?.let {
            Text(stringResource(RStrings.load_failed, it), color = MaterialTheme.colorScheme.error)
            if (state.poll == null) TextButton(onClick = onRetry) { Text(stringResource(RStrings.retry)) }
        }
        val poll = state.poll
        if (poll != null) {
            Text(poll.question, style = MaterialTheme.typography.bodyLarge)
            poll.choices.forEach { choice ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !state.submitting && !state.submitted) { onSelect(choice.id) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = state.choiceId == choice.id, onClick = { onSelect(choice.id) }, enabled = !state.submitting && !state.submitted)
                    Text(choice.text, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                    Text(stringResource(RStrings.reading_novel_poll_count, choice.count), style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(stringResource(RStrings.reading_novel_poll_count, poll.total), style = MaterialTheme.typography.labelSmall)
            Button(onClick = onAnswer, enabled = state.choiceId != null && !state.submitting && !state.submitted) {
                if (state.submitting) CircularProgressIndicator(Modifier.size(18.dp))
                Text(stringResource(if (state.submitted) RStrings.reading_novel_poll_voted else RStrings.reading_novel_poll_vote))
            }
        } else if (state.loaded && !state.loading && state.error == null) {
            Text(stringResource(RStrings.reading_novel_poll_absent), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PublicNovelCard(novel: PublicNovelPreview, onClick: (Long) -> Unit) {
    val id = novel.navigableId ?: return
    val blocked = BlockingRepositoryV2.collectNovelBlockAsState(id)
    val userBlocked = BlockingRepositoryV2.collectUserBlockAsState(novel.userId.toLongOrNull() ?: 0)
    if (blocked || userBlocked) return
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable { onClick(id) }) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (novel.url.isNotBlank()) AsyncImage(novel.url, novel.title, modifier = Modifier.size(72.dp), contentScale = ContentScale.Crop)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(novel.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(novel.userName, style = MaterialTheme.typography.bodyMedium)
                Text(novel.tags.joinToString(" · "), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun NovelRecommendationLoadState(state: LoadState, onRetry: () -> Unit, showEmpty: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when (state) {
            is LoadState.Loading -> CircularProgressIndicator()
            is LoadState.Error -> {
                Text(stringResource(RStrings.load_failed, state.error.message.orEmpty()))
                TextButton(onClick = onRetry) { Text(stringResource(RStrings.retry)) }
            }
            is LoadState.NotLoading -> if (showEmpty) Text(stringResource(RStrings.reading_novel_recommendations_empty))
        }
    }
}
