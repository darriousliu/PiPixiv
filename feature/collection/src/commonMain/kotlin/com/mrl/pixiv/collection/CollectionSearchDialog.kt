package com.mrl.pixiv.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.compose.IllustGridDefaults
import com.mrl.pixiv.common.compose.ui.illust.illustGrid
import com.mrl.pixiv.common.compose.ui.novel.NovelItem
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.collection.CollectionSearchOrder
import com.mrl.pixiv.common.data.collection.CollectionTagOptions
import com.mrl.pixiv.common.data.collection.CollectionWorkType
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** A modal destination also handles Android back and desktop Escape without changing the app graph. */
@Composable
fun CollectionSearchDialog(
    uid: Long,
    initialNovel: Boolean,
    onDismiss: () -> Unit,
    viewModel: CollectionSearchViewModel = koinViewModel { parametersOf(uid) },
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val illusts = viewModel.illusts.collectAsLazyPagingItems()
    val novels = viewModel.novels.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    var showFilters by remember { mutableStateOf(true) }
    LaunchedEffect(uid, initialNovel) {
        viewModel.open(if (initialNovel) CollectionWorkType.NOVEL else CollectionWorkType.ILLUST)
    }
    LaunchedEffect(state.query) {
        gridState.scrollToItem(0)
        listState.scrollToItem(0)
    }
    val novelMode = state.query.type == CollectionWorkType.NOVEL
    val refresh = if (novelMode) novels.loadState.refresh else illusts.loadState.refresh
    val append = if (novelMode) novels.loadState.append else illusts.loadState.append
    val count = if (novelMode) novels.itemCount else illusts.itemCount
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Scaffold(topBar = {
                TopAppBar(
                    title = { Text(stringResource(RStrings.discovery_collection_search)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(RStrings.cancel)) }
                    },
                    actions = {
                        IconButton(onClick = { showFilters = !showFilters }) { Icon(Icons.Rounded.FilterList, stringResource(RStrings.filter)) }
                        IconButton(onClick = {
                            if (state.sync != CollectionSyncState.READY) viewModel.checkSync()
                            else if (novelMode) novels.refresh() else illusts.refresh()
                        }) { Icon(Icons.Rounded.Refresh, stringResource(RStrings.discovery_retry)) }
                    },
                )
            }) { padding ->
                Column(Modifier.padding(padding).fillMaxSize()) {
                    when (state.sync) {
                        CollectionSyncState.CHECKING -> CircularProgressIndicator(Modifier.padding(24.dp))
                        CollectionSyncState.SYNCING, CollectionSyncState.FAILED, CollectionSyncState.NOT_OWNER -> {
                            Text(stringResource(when (state.sync) {
                                CollectionSyncState.SYNCING -> RStrings.discovery_collection_syncing
                                CollectionSyncState.NOT_OWNER -> RStrings.discovery_collection_self_only
                                else -> RStrings.discovery_collection_sync_failed
                            }), Modifier.padding(16.dp))
                            if (state.sync != CollectionSyncState.NOT_OWNER) TextButton(onClick = viewModel::checkSync) {
                                Text(stringResource(RStrings.discovery_retry))
                            }
                        }
                        CollectionSyncState.READY -> {
                            if (showFilters) CollectionSearchFilters(state, viewModel) {
                                viewModel.applyDraft()
                                showFilters = false
                            }
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                if (novelMode) {
                                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                                        items(novels.itemCount, key = { "${it}_${novels.peek(it)?.id}" }) { index ->
                                            novels[index]?.let { novel ->
                                                NovelItem(novel = novel,
                                                    onNovelClick = { onDismiss(); navigationManager.navigateToNovelDetailScreen(it) },
                                                    onSeriesClick = { onDismiss(); navigationManager.navigateToNovelSeriesScreen(it) },
                                                    onBookmarkClick = { bookmarked, restrict, tags ->
                                                        if (bookmarked) BookmarkState.deleteBookmarkNovel(novel.id)
                                                        else BookmarkState.bookmarkNovel(novel.id, restrict, tags)
                                                    })
                                            }
                                        }
                                    }
                                } else {
                                    val layout = IllustGridDefaults.relatedLayoutParameters()
                                    LazyVerticalGrid(columns = layout.gridCells, state = gridState, modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(8.dp), verticalArrangement = layout.verticalArrangement,
                                        horizontalArrangement = layout.horizontalArrangement) {
                                        illustGrid(illusts = illusts, navToPictureScreen = { works, index, prefix, transition ->
                                            onDismiss()
                                            navigationManager.navigateToPictureScreen(works, index, prefix, transition)
                                        })
                                    }
                                }
                                if (refresh is LoadState.Loading && count == 0) CircularProgressIndicator(Modifier.align(Alignment.Center))
                                else if (refresh is LoadState.Error || (refresh is LoadState.NotLoading && count == 0)) {
                                    Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(stringResource(if (refresh is LoadState.Error) RStrings.discovery_load_failed else RStrings.discovery_no_results))
                                        if (refresh is LoadState.Error) TextButton(onClick = { if (novelMode) novels.retry() else illusts.retry() }) {
                                            Text(stringResource(RStrings.discovery_retry))
                                        }
                                    }
                                }
                            }
                            if (append is LoadState.Loading) CircularProgressIndicator(Modifier.padding(8.dp))
                            if (append is LoadState.Error) TextButton(onClick = { if (novelMode) novels.retry() else illusts.retry() }) {
                                Text(stringResource(RStrings.discovery_retry))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionSearchFilters(state: CollectionSearchState, viewModel: CollectionSearchViewModel, onApply: () -> Unit) {
    val draft = state.draft
    Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(draft.restrict == Restrict.PUBLIC, { viewModel.updateDraft(draft.copy(restrict = Restrict.PUBLIC, period = null)) },
                label = { Text(stringResource(RStrings.word_public)) })
            FilterChip(draft.restrict == Restrict.PRIVATE, { viewModel.updateDraft(draft.copy(restrict = Restrict.PRIVATE, period = null)) },
                label = { Text(stringResource(RStrings.word_private)) })
        }
        OutlinedTextField(draft.bookmarkTag, { viewModel.updateDraft(draft.copy(bookmarkTag = it.take(100))) },
            modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(RStrings.bookmark_tags)) })
        CollectionTagSuggestions(state.bookmarkTags, state.optionsLoading, { viewModel.updateDraft(draft.copy(bookmarkTag = it)) },
            { viewModel.moreTags(true) })
        OutlinedTextField(draft.workTag, { viewModel.updateDraft(draft.copy(workTag = it.take(100))) },
            modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(RStrings.discovery_work_tag)) })
        CollectionTagSuggestions(state.workTags, state.optionsLoading, { viewModel.updateDraft(draft.copy(workTag = it)) },
            { viewModel.moreTags(false) })
        var monthsExpanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { monthsExpanded = true }) {
                Text("${stringResource(RStrings.discovery_bookmark_month)}: ${draft.period ?: stringResource(RStrings.all)}")
            }
            DropdownMenu(monthsExpanded, { monthsExpanded = false }) {
                DropdownMenuItem(text = { Text(stringResource(RStrings.all)) }, onClick = {
                    viewModel.updateDraft(draft.copy(period = null)); monthsExpanded = false
                })
                state.periods.forEach { period ->
                    DropdownMenuItem(text = { Text("${period.name} (${period.count})") }, onClick = {
                        viewModel.updateDraft(draft.copy(period = period.name)); monthsExpanded = false
                    })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CollectionSearchOrder.entries.forEach { order ->
                FilterChip(draft.order == order, { viewModel.updateDraft(draft.copy(order = order)) }, label = {
                    Text(stringResource(if (order == CollectionSearchOrder.NEWEST) RStrings.discovery_bookmarked_newest else RStrings.discovery_bookmarked_oldest))
                })
            }
        }
        if (state.optionsLoading) CircularProgressIndicator()
        if (state.optionsFailed) {
            Text(stringResource(RStrings.discovery_options_failed), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { viewModel.loadOptions() }) { Text(stringResource(RStrings.discovery_retry)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onApply) { Text(stringResource(RStrings.apply)) }
            TextButton(onClick = viewModel::resetDraft) { Text(stringResource(RStrings.discovery_clear_filters)) }
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
