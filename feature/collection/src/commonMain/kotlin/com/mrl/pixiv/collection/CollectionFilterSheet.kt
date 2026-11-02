package com.mrl.pixiv.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import com.mrl.pixiv.strings.all
import com.mrl.pixiv.strings.apply
import com.mrl.pixiv.strings.bookmark_tags
import com.mrl.pixiv.strings.discovery_advanced_filters
import com.mrl.pixiv.strings.discovery_bookmark_month
import com.mrl.pixiv.strings.discovery_bookmark_tag_list
import com.mrl.pixiv.strings.discovery_bookmarked_newest
import com.mrl.pixiv.strings.discovery_bookmarked_oldest
import com.mrl.pixiv.strings.discovery_clear_filters
import com.mrl.pixiv.strings.discovery_collection_filters
import com.mrl.pixiv.strings.discovery_collection_sync_failed
import com.mrl.pixiv.strings.discovery_collection_syncing
import com.mrl.pixiv.strings.discovery_load_more
import com.mrl.pixiv.strings.discovery_options_failed
import com.mrl.pixiv.strings.discovery_retry
import com.mrl.pixiv.strings.discovery_tag_suggestions
import com.mrl.pixiv.strings.discovery_work_tag
import com.mrl.pixiv.strings.word_private
import com.mrl.pixiv.strings.word_public
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
        collectionViewModel.dispatch(
            if (novel) CollectionAction.LoadUserBookmarksTagsNovel(restrict)
            else CollectionAction.LoadUserBookmarksTagsIllust(restrict)
        )
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
            tagPage = collectionState.tagPages[CollectionTagKey(novel, restrict)]
                ?: CollectionTagPage(),
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
        Text(
            stringResource(RStrings.discovery_collection_filters),
            Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
            style = MaterialTheme.typography.titleLarge
        )
        Column(
            Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isOwner) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    draft.restrict == Restrict.PUBLIC,
                    { onDraftChange(draft.copy(restrict = Restrict.PUBLIC, period = null)) },
                    label = { Text(stringResource(RStrings.word_public)) })
                FilterChip(
                    draft.restrict == Restrict.PRIVATE,
                    { onDraftChange(draft.copy(restrict = Restrict.PRIVATE, period = null)) },
                    label = { Text(stringResource(RStrings.word_private)) })
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    draft.bookmarkTag,
                    { onDraftChange(draft.copy(bookmarkTag = it.take(100))) },
                    modifier = Modifier.fillMaxWidth().testTag("collection-bookmark-tag"),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Rounded.Tag, null, Modifier.size(20.dp)) },
                    label = { Text(stringResource(RStrings.bookmark_tags)) })
                if (isOwner && state.sync == CollectionSyncState.READY && draft.bookmarkTag.isNotBlank()) {
                    CollectionTagSuggestions(
                        state.bookmarkTags,
                        state.optionsLoading,
                        { onDraftChange(draft.copy(bookmarkTag = it)) },
                        { onMoreSuggestions(true) })
                }
            }
            CollectionBookmarkTagMenu(bookmarkTags, tagPage, onLoadMoreTags) {
                onDraftChange(draft.copy(bookmarkTag = it.orEmpty()))
            }
            if (isOwner) {
                HorizontalDivider()
                when (state.sync) {
                    CollectionSyncState.CHECKING -> Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }

                    CollectionSyncState.SYNCING, CollectionSyncState.FAILED -> {
                        CollectionFilterNotice(
                            message = stringResource(
                                if (state.sync == CollectionSyncState.SYNCING)
                                    RStrings.discovery_collection_syncing else RStrings.discovery_collection_sync_failed
                            ),
                            onRetry = onRetrySync,
                        )
                    }

                    else -> Unit
                }
                if (state.sync == CollectionSyncState.READY) {
                    Text(
                        stringResource(RStrings.discovery_advanced_filters),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            draft.workTag,
                            { onDraftChange(draft.copy(workTag = it.take(100))) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            leadingIcon = { Icon(Icons.Rounded.Tag, null, Modifier.size(20.dp)) },
                            label = { Text(stringResource(RStrings.discovery_work_tag)) })
                        CollectionTagSuggestions(
                            state.workTags,
                            state.optionsLoading,
                            { onDraftChange(draft.copy(workTag = it)) },
                            { onMoreSuggestions(false) })
                    }
                    var monthsExpanded by remember { mutableStateOf(false) }
                    Box {
                        CollectionFilterChoice(
                            label = stringResource(RStrings.discovery_bookmark_month),
                            value = draft.period ?: stringResource(RStrings.all),
                            onClick = { monthsExpanded = true },
                        )
                        DropdownMenu(monthsExpanded, { monthsExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(RStrings.all)) },
                                onClick = {
                                    onDraftChange(draft.copy(period = null)); monthsExpanded = false
                                })
                            state.periods.forEach { period ->
                                DropdownMenuItem(
                                    text = { Text("${period.name} (${period.count})") },
                                    onClick = {
                                        onDraftChange(draft.copy(period = period.name)); monthsExpanded =
                                        false
                                    })
                            }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CollectionSearchOrder.entries.forEach { order ->
                            FilterChip(
                                draft.order == order,
                                { onDraftChange(draft.copy(order = order)) },
                                label = {
                                    Text(
                                        stringResource(
                                            if (order == CollectionSearchOrder.NEWEST)
                                                RStrings.discovery_bookmarked_newest else RStrings.discovery_bookmarked_oldest
                                        )
                                    )
                                })
                        }
                    }
                    if (state.optionsLoading) Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                    if (state.optionsFailed) CollectionFilterNotice(
                        message = stringResource(RStrings.discovery_options_failed),
                        onRetry = onRetryOptions,
                    )
                }
            }
        }
        HorizontalDivider(
            Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onReset,
                modifier = Modifier.testTag("collection-filter-reset")
            ) { Text(stringResource(RStrings.discovery_clear_filters)) }
            Button(
                onClick = onApply,
                enabled = canApplyCollectionDraft(state),
                modifier = Modifier.weight(1f).testTag("collection-filter-apply")
            ) { Text(stringResource(RStrings.apply)) }
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
        CollectionFilterChoice(
            label = stringResource(RStrings.discovery_bookmark_tag_list),
            onClick = { expanded = true })
        DropdownMenu(expanded, { expanded = false }) {
            tags.forEach { tag ->
                DropdownMenuItem(
                    text = { Text(tag.displayName + (tag.count?.let { " ($it)" } ?: "")) },
                    onClick = { onSelect(tag.name); expanded = false })
            }
            if (page.loading) DropdownMenuItem(text = {
                CircularProgressIndicator(
                    Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }, enabled = false, onClick = {})
            if (page.failed) DropdownMenuItem(
                text = { Text(stringResource(RStrings.discovery_retry)) },
                onClick = onMore
            )
            else if (page.nextUrl != null && !page.loading) DropdownMenuItem(
                text = { Text(stringResource(RStrings.discovery_load_more)) }, onClick = onMore
            )
        }
    }
}

@Composable
private fun CollectionTagSuggestions(
    options: CollectionTagOptions,
    loading: Boolean,
    onSelect: (String) -> Unit,
    onMore: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, enabled = !loading && options.tags.isNotEmpty()) {
            Text(stringResource(RStrings.discovery_tag_suggestions))
        }
        DropdownMenu(expanded, { expanded = false }) {
            options.tags.filter { it.name.length <= 100 }.forEach { tag ->
                DropdownMenuItem(
                    text = { Text("${tag.name} (${tag.count})") },
                    onClick = { onSelect(tag.name); expanded = false })
            }
            if (options.nextUrl != null) DropdownMenuItem(
                text = { Text(stringResource(RStrings.discovery_load_more)) },
                enabled = !loading,
                onClick = onMore
            )
        }
    }
}

@Composable
private fun CollectionFilterChoice(label: String, value: String? = null, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            value?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.ExpandMore,
                null,
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CollectionFilterNotice(message: String, onRetry: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(RStrings.discovery_retry))
            }
        }
    }
}
