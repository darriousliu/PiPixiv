package com.mrl.pixiv.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.collection.CollectionSearchOrder
import com.mrl.pixiv.common.data.collection.CollectionSearchQuery
import com.mrl.pixiv.common.data.collection.CollectionTagOptions
import com.mrl.pixiv.common.data.collection.CollectionWorkType
import com.mrl.pixiv.common.repository.isSelf
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.strings.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Edits a draft only. Results stay in the collection screen and use its existing scroll state. */
@Composable
fun CollectionFilterSheet(
    uid: Long,
    query: CollectionSearchQuery,
    collectionViewModel: CollectionViewModel,
    onDismiss: () -> Unit,
    onApply: (CollectionSearchQuery) -> Unit,
    viewModel: CollectionSearchViewModel = koinViewModel { parametersOf(uid) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val collectionState = collectionViewModel.asState()
    LaunchedEffect(query, uid) { viewModel.open(query) }
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }
    val novel = state.draft.type == CollectionWorkType.NOVEL
    val restrict = state.draft.restrict
    LaunchedEffect(novel, restrict) {
        collectionViewModel.dispatch(if (novel) CollectionAction.LoadUserBookmarksTagsNovel(restrict)
            else CollectionAction.LoadUserBookmarksTagsIllust(restrict))
    }
    val bookmarkTags = when {
        novel && restrict == Restrict.PUBLIC -> collectionState.userBookmarkTagsNovel
        novel -> collectionState.privateBookmarkTagsNovel
        restrict == Restrict.PUBLIC -> collectionState.userBookmarkTagsIllust
        else -> collectionState.privateBookmarkTagsIllust
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        CollectionFilterSheetContent(
            state = state,
            isOwner = uid.isSelf,
            bookmarkTags = bookmarkTags,
            tagPage = collectionState.tagPages[CollectionTagKey(novel, restrict)] ?: CollectionTagPage(),
            onLoadMoreTags = { collectionViewModel.loadMoreTags(novel, restrict) },
            onDraftChange = viewModel::updateDraft,
            onRetrySync = viewModel::checkSync,
            onRetryOptions = { viewModel.loadOptions() },
            onMoreSuggestions = viewModel::moreTags,
            onReset = viewModel::resetDraft,
            onApply = { viewModel.applyDraft()?.let { onApply(it); onDismiss() } },
        )
    }
}

@Composable
internal fun CollectionFilterSheetContent(
    state: CollectionSearchState,
    isOwner: Boolean,
    bookmarkTags: List<RestrictBookmarkTag>,
    tagPage: CollectionTagPage,
    onLoadMoreTags: () -> Unit,
    onDraftChange: (CollectionSearchQuery) -> Unit,
    onRetrySync: () -> Unit,
    onRetryOptions: () -> Unit,
    onMoreSuggestions: (Boolean) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
) {
    val draft = state.draft
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(RStrings.discovery_collection_filters), Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge)
        Column(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 560.dp)
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isOwner) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(draft.restrict == Restrict.PUBLIC,
                    { onDraftChange(draft.copy(restrict = Restrict.PUBLIC, period = null)) },
                    label = { Text(stringResource(RStrings.word_public)) })
                FilterChip(draft.restrict == Restrict.PRIVATE,
                    { onDraftChange(draft.copy(restrict = Restrict.PRIVATE, period = null)) },
                    label = { Text(stringResource(RStrings.word_private)) })
            }
            OutlinedTextField(draft.bookmarkTag, { onDraftChange(draft.copy(bookmarkTag = it.take(100))) },
                modifier = Modifier.fillMaxWidth().testTag("collection-bookmark-tag"), singleLine = true, label = { Text(stringResource(RStrings.bookmark_tags)) })
            CollectionBookmarkTagMenu(bookmarkTags, tagPage, onLoadMoreTags) {
                onDraftChange(draft.copy(bookmarkTag = it.orEmpty()))
            }
            if (isOwner) {
                HorizontalDivider()
                when (state.sync) {
                    CollectionSyncState.CHECKING -> CircularProgressIndicator()
                    CollectionSyncState.SYNCING, CollectionSyncState.FAILED -> {
                        Text(stringResource(if (state.sync == CollectionSyncState.SYNCING)
                            RStrings.discovery_collection_syncing else RStrings.discovery_collection_sync_failed))
                        TextButton(onClick = onRetrySync) { Text(stringResource(RStrings.discovery_retry)) }
                    }
                    else -> Unit
                }
                if (state.sync == CollectionSyncState.READY) {
                    if (draft.bookmarkTag.isNotBlank()) CollectionTagSuggestions(state.bookmarkTags, state.optionsLoading,
                        { onDraftChange(draft.copy(bookmarkTag = it)) }, { onMoreSuggestions(true) })
                    OutlinedTextField(draft.workTag, { onDraftChange(draft.copy(workTag = it.take(100))) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(stringResource(RStrings.discovery_work_tag)) })
                    CollectionTagSuggestions(state.workTags, state.optionsLoading,
                        { onDraftChange(draft.copy(workTag = it)) }, { onMoreSuggestions(false) })
                    var monthsExpanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { monthsExpanded = true }) {
                            Text("${stringResource(RStrings.discovery_bookmark_month)}: ${draft.period ?: stringResource(RStrings.all)}")
                        }
                        DropdownMenu(monthsExpanded, { monthsExpanded = false }) {
                            DropdownMenuItem(text = { Text(stringResource(RStrings.all)) }, onClick = {
                                onDraftChange(draft.copy(period = null)); monthsExpanded = false
                            })
                            state.periods.forEach { period ->
                                DropdownMenuItem(text = { Text("${period.name} (${period.count})") }, onClick = {
                                    onDraftChange(draft.copy(period = period.name)); monthsExpanded = false
                                })
                            }
                        }
                    }
                    Column {
                        CollectionSearchOrder.entries.forEach { order ->
                            FilterChip(draft.order == order, { onDraftChange(draft.copy(order = order)) }, label = {
                                Text(stringResource(if (order == CollectionSearchOrder.NEWEST)
                                    RStrings.discovery_bookmarked_newest else RStrings.discovery_bookmarked_oldest))
                            })
                        }
                    }
                    if (state.optionsLoading) CircularProgressIndicator()
                    if (state.optionsFailed) {
                        Text(stringResource(RStrings.discovery_options_failed), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onRetryOptions) { Text(stringResource(RStrings.discovery_retry)) }
                    }
                }
            }
        }
        Row(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onApply, enabled = canApplyCollectionDraft(state), modifier = Modifier.testTag("collection-filter-apply")) { Text(stringResource(RStrings.apply)) }
            TextButton(onClick = onReset, modifier = Modifier.testTag("collection-filter-reset")) { Text(stringResource(RStrings.discovery_clear_filters)) }
        }
    }
}

@Composable
private fun CollectionBookmarkTagMenu(
    tags: List<RestrictBookmarkTag>,
    page: CollectionTagPage,
    onMore: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text(stringResource(RStrings.discovery_bookmark_tag_list)) }
        DropdownMenu(expanded, { expanded = false }) {
            tags.forEach { tag ->
                DropdownMenuItem(text = { Text(tag.displayName + (tag.count?.let { " ($it)" } ?: "")) },
                    onClick = { onSelect(tag.name); expanded = false })
            }
            if (page.loading) DropdownMenuItem(text = { CircularProgressIndicator() }, enabled = false, onClick = {})
            if (page.failed) DropdownMenuItem(text = { Text(stringResource(RStrings.discovery_retry)) }, onClick = onMore)
            else if (page.nextUrl != null && !page.loading) DropdownMenuItem(
                text = { Text(stringResource(RStrings.discovery_load_more)) }, onClick = onMore)
        }
    }
}

@Composable
private fun CollectionTagSuggestions(options: CollectionTagOptions, loading: Boolean, onSelect: (String) -> Unit, onMore: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, enabled = !loading && options.tags.isNotEmpty()) {
            Text(stringResource(RStrings.discovery_tag_suggestions))
        }
        DropdownMenu(expanded, { expanded = false }) {
            options.tags.filter { it.name.length <= 100 }.forEach { tag ->
                DropdownMenuItem(text = { Text("${tag.name} (${tag.count})") }, onClick = { onSelect(tag.name); expanded = false })
            }
            if (options.nextUrl != null) DropdownMenuItem(
                text = { Text(stringResource(RStrings.discovery_load_more)) }, enabled = !loading, onClick = onMore)
        }
    }
}
