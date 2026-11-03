package com.mrl.pixiv.search.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.compose.layout.LocalMainNavigationBottomInset
import com.mrl.pixiv.common.compose.ui.ContentEmptyState
import com.mrl.pixiv.common.compose.ui.ContentErrorState
import com.mrl.pixiv.common.compose.ui.ContentLoadingState
import com.mrl.pixiv.common.compose.ui.ContentPullToRefreshBox
import com.mrl.pixiv.common.compose.ui.ViewModeToggleButton
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.repository.BookmarkedTagRepository
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.search.preview.components.TrendingItem
import com.mrl.pixiv.strings.bookmark_tags
import com.mrl.pixiv.strings.enter_keywords
import com.mrl.pixiv.strings.popular_tags
import com.mrl.pixiv.strings.view_all
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchPreviewScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchPreviewViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val state = viewModel.asState()
    val bookmarkedTags by BookmarkedTagRepository.bookmarkedTags.collectAsStateWithLifecycle()
    val appViewMode by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle { appViewMode }
    val scope = rememberCoroutineScope()

    SearchPreviewContent(
        state = state,
        bookmarkedTags = bookmarkedTags,
        appViewMode = appViewMode,
        onOpenSearch = navigationManager::navigateToSearchScreen,
        onOpenBookmarkedTags = navigationManager::navigateToBookmarkedTagsScreen,
        onSearchTag = { keyword ->
            viewModel.dispatch(SearchPreviewAction.AddSearchHistory(keyword, appViewMode))
            navigationManager.navigateToSearchResultScreen(
                searchWord = keyword,
                searchMode = appViewMode,
            )
        },
        onRefresh = { viewModel.dispatch(SearchPreviewAction.LoadTrendingTags) },
        onModeChange = { mode ->
            viewModel.switchViewMode(mode)
            scope.launch { viewModel.lazyGridState.scrollToItem(0) }
        },
        modifier = modifier,
        gridState = viewModel.lazyGridState,
    )
}

@Composable
internal fun SearchPreviewContent(
    state: SearchPreviewState,
    bookmarkedTags: List<Tag>,
    appViewMode: AppViewMode,
    onOpenSearch: () -> Unit,
    onOpenBookmarkedTags: () -> Unit,
    onSearchTag: (String) -> Unit,
    onRefresh: () -> Unit,
    onModeChange: (AppViewMode) -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    TextField(
                        value = TextFieldValue(),
                        onValueChange = {},
                        modifier = Modifier
                            .heightIn(min = 56.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .throttleClick(onClick = onOpenSearch),
                        placeholder = { Text(stringResource(RStrings.enter_keywords)) },
                        colors = TextFieldDefaults.colors(disabledIndicatorColor = Color.Transparent),
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                        enabled = false,
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    )
                },
            )
        },
        floatingActionButton = {
            ViewModeToggleButton(
                currentMode = appViewMode,
                onModeChange = onModeChange,
                modifier = Modifier.padding(bottom = LocalMainNavigationBottomInset.current),
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
    ) { innerPadding ->
        ContentPullToRefreshBox(
            hasContent = bookmarkedTags.isNotEmpty() || state.trendingTags.isNotEmpty(),
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(innerPadding),
            state = pullRefreshState,
        ) {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                columns = GridCells.Adaptive(minSize = 104.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
                    bottom = 88.dp + maxOf(
                        LocalMainNavigationBottomInset.current,
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "bookmarked_tags", span = { GridItemSpan(maxLineSpan) }) {
                    BookmarkedTagsSection(
                        tags = bookmarkedTags,
                        onSearch = onSearchTag,
                        onViewAll = onOpenBookmarkedTags,
                    )
                }
                item(key = "popular_tags_title", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(RStrings.popular_tags),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                if (state.trendingTags.isEmpty() || state.failed && !state.refreshing) {
                    item(key = "trending_status", span = { GridItemSpan(maxLineSpan) }) {
                        TrendingTagsStatus(state = state, onRetry = onRefresh)
                    }
                }
                items(items = state.trendingTags, key = { "trending_${it.tag}" }) { tag ->
                    TrendingItem(trendingTag = tag, onSearch = onSearchTag)
                }
            }
        }
    }
}

@Composable
private fun BookmarkedTagsSection(
    tags: List<Tag>,
    onSearch: (String) -> Unit,
    onViewAll: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(RStrings.bookmark_tags),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (tags.isNotEmpty()) {
                    TextButton(onClick = onViewAll) { Text(stringResource(RStrings.view_all)) }
                }
            }
            if (tags.isEmpty()) {
                ContentEmptyState(
                    icon = Icons.Rounded.Style,
                    compact = true,
                    action = {
                        TextButton(onClick = onViewAll) { Text(stringResource(RStrings.view_all)) }
                    },
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.take(6).forEach { tag ->
                        SuggestionChip(
                            onClick = { onSearch(tag.name) },
                            label = {
                                Text(
                                    text = "#${tag.name}",
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingTagsStatus(
    state: SearchPreviewState,
    onRetry: () -> Unit,
) {
    when {
        state.trendingTags.isEmpty() && state.refreshing -> ContentLoadingState()
        state.failed && !state.refreshing -> ContentErrorState(onRetry = onRetry, compact = true)
        state.trendingTags.isEmpty() -> ContentEmptyState()
    }
}
