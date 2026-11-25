package com.mrl.pixiv.search.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.compose.IllustGridSpacing
import com.mrl.pixiv.common.compose.layout.LocalMainNavigationBottomInset
import com.mrl.pixiv.common.compose.layout.currentPaneLayoutInfo
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastMedium
import com.mrl.pixiv.common.compose.ui.ContentEmptyState
import com.mrl.pixiv.common.compose.ui.ContentErrorState
import com.mrl.pixiv.common.compose.ui.ContentLoadingState
import com.mrl.pixiv.common.compose.ui.ContentPullToRefreshBox
import com.mrl.pixiv.common.compose.ui.SectionHeader
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
    val paneInfo = currentPaneLayoutInfo()
    val edgePadding = IllustGridSpacing.edgePadding(paneInfo)
    val rowSpacing = IllustGridSpacing.LabeledRow

    // 框架调整时同步 feature/setting/.../appearance/MainPageSkeletons.kt 中的 SearchSkeleton。
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    // 顶栏操作区自带 4dp 尾部留白，搜索入口与下方网格使用相同的页边距。
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(start = edgePadding, end = edgePadding - 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            onClick = throttleClick(onClick = onOpenSearch),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .widthIn(max = 720.dp)
                                .fillMaxWidth()
                                .height(56.dp)
                                .semantics { role = Role.Button },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(24.dp))
                                Text(
                                    text = stringResource(RStrings.enter_keywords),
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                },
                contentPadding = PaddingValues(0.dp),
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
                columns = GridCells.Adaptive(minSize = if (paneInfo.sizeClass.isWidthAtLeastMedium) 140.dp else 104.dp),
                contentPadding = PaddingValues(
                    start = edgePadding,
                    top = 8.dp,
                    end = edgePadding,
                    // 最后一行卡片或状态项已经包含行末留白。
                    bottom = 88.dp - rowSpacing + maxOf(
                        LocalMainNavigationBottomInset.current,
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
                ),
                horizontalArrangement = Arrangement.spacedBy(IllustGridSpacing.LabeledColumn),
            ) {
                item(key = "bookmarked_tags", span = { GridItemSpan(maxLineSpan) }) {
                    BookmarkedTagsSection(
                        tags = bookmarkedTags,
                        onSearch = onSearchTag,
                        onViewAll = onOpenBookmarkedTags,
                    )
                }
                item(key = "popular_tags_title", span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(
                        text = stringResource(RStrings.popular_tags),
                        modifier = Modifier.padding(top = rowSpacing + 8.dp),
                    )
                }
                if (state.trendingTags.isEmpty() || state.failed && !state.refreshing) {
                    item(key = "trending_status", span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.padding(vertical = rowSpacing)) {
                            TrendingTagsStatus(state = state, onRetry = onRefresh)
                        }
                    }
                }
                items(items = state.trendingTags, key = { "trending_${it.tag}" }) { tag ->
                    TrendingItem(
                        trendingTag = tag,
                        onSearch = onSearchTag,
                        modifier = Modifier.padding(bottom = rowSpacing),
                    )
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
    val viewAll: @Composable () -> Unit = {
        TextButton(onClick = onViewAll) { Text(stringResource(RStrings.view_all)) }
    }
    Column {
        SectionHeader(
            text = stringResource(RStrings.bookmark_tags),
            action = if (tags.isNotEmpty()) viewAll else null,
        )
        if (tags.isEmpty()) {
            ContentEmptyState(
                icon = Icons.Rounded.Style,
                compact = true,
                action = viewAll,
            )
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val chipMaxWidth = maxWidth
                FlexBox(
                    modifier = Modifier.fillMaxWidth(),
                    config = {
                        wrap(FlexWrap.Wrap)
                        gap(row = 0.dp, column = 8.dp)
                    },
                ) {
                    tags.take(6).forEach { tag ->
                        SuggestionChip(
                            onClick = { onSearch(tag.name) },
                            modifier = Modifier.widthIn(max = chipMaxWidth),
                            label = {
                                Text(
                                    text = "#${tag.name}",
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
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
