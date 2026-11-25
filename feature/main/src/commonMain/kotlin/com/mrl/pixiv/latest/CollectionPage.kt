package com.mrl.pixiv.latest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.collection.CollectionFilterSheet
import com.mrl.pixiv.collection.CollectionResultsStatus
import com.mrl.pixiv.collection.CollectionViewModel
import com.mrl.pixiv.common.compose.RecommendGridDefaults
import com.mrl.pixiv.common.compose.layout.AdaptiveVerticalStaggeredGrid
import com.mrl.pixiv.common.compose.layout.LocalMainNavigationBottomInset
import com.mrl.pixiv.common.compose.layout.withMainNavigationBottomInset
import com.mrl.pixiv.common.compose.listener.KeyEventListener
import com.mrl.pixiv.common.compose.listener.keyboardScrollerController
import com.mrl.pixiv.common.compose.ui.ContentPullToRefreshBox
import com.mrl.pixiv.common.compose.ui.PagingAppendState
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.compose.ui.illust.RectangleIllustItem
import com.mrl.pixiv.common.compose.ui.novel.NovelItem
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.kts.itemIndexKey
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.isSelf
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.viewmodel.asState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CollectionPage(
    uid: Long,
    refreshFlow: SharedFlow<LatestPage>,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = koinViewModel { parametersOf(uid) },
    latestViewModel: LatestViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val appViewMode by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle { appViewMode }

    when (appViewMode) {
        AppViewMode.ILLUST -> {
            CollectionIllustPage(
                uid = uid,
                refreshFlow = refreshFlow,
                modifier = modifier,
                viewModel = viewModel,
                latestViewModel = latestViewModel,
                navigationManager = navigationManager
            )
        }

        AppViewMode.NOVEL -> {
            CollectionNovelPage(
                uid = uid,
                refreshFlow = refreshFlow,
                modifier = modifier,
                viewModel = viewModel,
                latestViewModel = latestViewModel,
                navigationManager = navigationManager
            )
        }
    }
}

@Composable
private fun CollectionIllustPage(
    uid: Long,
    refreshFlow: SharedFlow<LatestPage>,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = koinViewModel { parametersOf(uid) },
    latestViewModel: LatestViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val userBookmarksIllusts = viewModel.userBookmarksIllusts.collectAsLazyPagingItems()
    val pullRefreshState = rememberPullToRefreshState()
    val lazyGridState = latestViewModel.collectionLazyGirdState
    val state = viewModel.asState()
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val layoutParams = RecommendGridDefaults.coverLayoutParameters()
    val isRefreshing = userBookmarksIllusts.loadState.refresh is LoadState.Loading
    val controller = remember {
        keyboardScrollerController(lazyGridState) {
            lazyGridState.layoutInfo.viewportSize.height.toFloat()
        }
    }

    KeyEventListener(controller)

    LaunchedEffect(Unit) {
        refreshFlow.collect {
            userBookmarksIllusts.refresh()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LatestFilterRow(
            selected = state.restrict,
            onSelect = { restrict ->
                viewModel.updateFilterTag(restrict, state.filterTag)
                if (state.restrict != restrict) scope.launch { lazyGridState.scrollToItem(0) }
            },
            showPrivate = uid.isSelf,
            onOpenFilters = { showFilterDialog = true },
        )
        ContentPullToRefreshBox(
            hasContent = userBookmarksIllusts.itemCount > 0,
            isRefreshing = isRefreshing,
            onRefresh = { userBookmarksIllusts.refresh() },
            modifier = Modifier.fillMaxSize(),
            state = pullRefreshState,
        ) {
            AdaptiveVerticalStaggeredGrid(
                columns = layoutParams.gridCells,
                modifier = Modifier.fillMaxSize(),
                state = lazyGridState,
                contentPadding = PaddingValues(
                    start = layoutParams.edgePadding,
                    top = 12.dp,
                    end = layoutParams.edgePadding,
                ).withMainNavigationBottomInset(),
                verticalItemSpacing = layoutParams.verticalArrangement.spacing,
                horizontalArrangement = layoutParams.horizontalArrangement,
            ) {
                items(
                    count = userBookmarksIllusts.itemCount,
                    key = userBookmarksIllusts.itemIndexKey { index, item -> "${index}_${item.id}" }
                ) { index ->
                    val illust = userBookmarksIllusts[index] ?: return@items
                    val isBookmarked = illust.isBookmark
                    RectangleIllustItem(
                        illust = illust,
                        isBookmarked = isBookmarked,
                        navToPictureScreen = { cover ->
                            navigationManager.navigateToPictureScreen(
                                userBookmarksIllusts.itemSnapshotList.items,
                                index,
                                cover,
                            )
                        },
                        onBookmarkClick = { restrict, tags, isEdit ->
                            if (isEdit || !isBookmarked) {
                                BookmarkState.bookmarkIllust(illust.id, restrict, tags)
                            } else {
                                BookmarkState.deleteBookmarkIllust(illust.id)
                            }
                        },
                        sharedElementSlot = "latest-bookmarks:$index",
                    )
                }

                if (
                    userBookmarksIllusts.itemCount > 0 &&
                    userBookmarksIllusts.loadState.refresh is LoadState.NotLoading &&
                    userBookmarksIllusts.loadState.append !is LoadState.NotLoading
                ) {
                    item(key = "append_state", span = StaggeredGridItemSpan.FullLine) {
                        PagingAppendState(
                            loadStates = userBookmarksIllusts.loadState,
                            itemCount = userBookmarksIllusts.itemCount,
                            onRetry = userBookmarksIllusts::retry,
                            modifier = Modifier.padding(bottom = 136.dp),
                        )
                    }
                }
            }
            CollectionResultsStatus(
                loadStates = userBookmarksIllusts.loadState,
                itemCount = userBookmarksIllusts.itemCount,
                onRetry = userBookmarksIllusts::retry,
                modifier = Modifier.padding(
                    bottom = LocalMainNavigationBottomInset.current +
                            if (userBookmarksIllusts.itemCount > 0) 136.dp else 0.dp,
                ),
            )
            VerticalScrollbar(
                state = lazyGridState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
    if (showFilterDialog) {
        CollectionFilterSheet(
            uid = uid,
            query = state.illustQuery,
            collectionViewModel = viewModel,
            onDismiss = { showFilterDialog = false },
            onApply = { query ->
                viewModel.applyFilter(query)
                if (query != state.illustQuery) scope.launch { lazyGridState.scrollToItem(0) }
            },
        )
    }

}

@Composable
private fun CollectionNovelPage(
    uid: Long,
    refreshFlow: SharedFlow<LatestPage>,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = koinViewModel { parametersOf(uid) },
    latestViewModel: LatestViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val userBookmarksNovels = viewModel.userBookmarksNovels.collectAsLazyPagingItems()
    val pullRefreshState = rememberPullToRefreshState()
    val lazyListState = latestViewModel.collectionNovelLazyListState
    val state = viewModel.asState()
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isRefreshing = userBookmarksNovels.loadState.refresh is LoadState.Loading
    val controller = remember {
        keyboardScrollerController(lazyListState) {
            lazyListState.layoutInfo.viewportSize.height.toFloat()
        }
    }

    KeyEventListener(controller)

    LaunchedEffect(Unit) {
        refreshFlow.collect {
            userBookmarksNovels.refresh()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LatestFilterRow(
            selected = state.novelRestrict,
            onSelect = { restrict ->
                viewModel.updateNovelFilterTag(restrict, state.novelFilterTag)
                if (state.novelRestrict != restrict) scope.launch { lazyListState.scrollToItem(0) }
            },
            showPrivate = uid.isSelf,
            onOpenFilters = { showFilterDialog = true },
        )
        ContentPullToRefreshBox(
            hasContent = userBookmarksNovels.itemCount > 0,
            isRefreshing = isRefreshing,
            onRefresh = { userBookmarksNovels.refresh() },
            modifier = Modifier.fillMaxSize(),
            state = pullRefreshState,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = PaddingValues().withMainNavigationBottomInset(),
            ) {
                items(
                    count = userBookmarksNovels.itemCount,
                    key = userBookmarksNovels.itemIndexKey { index, item -> "${index}_${item.id}" }
                ) { index ->
                    userBookmarksNovels[index]?.let { novel ->
                        NovelItem(
                            novel = novel,
                            onNovelClick = { novelId ->
                                navigationManager.navigateToNovelDetailScreen(novelId)
                            },
                            onSeriesClick = navigationManager::navigateToNovelSeriesScreen,
                            onBookmarkClick = { isBookmarked, restrict, tags ->
                                if (isBookmarked) {
                                    BookmarkState.deleteBookmarkNovel(novel.id)
                                } else {
                                    BookmarkState.bookmarkNovel(novel.id, restrict, tags)
                                }
                            }
                        )
                    }
                }

                if (
                    userBookmarksNovels.itemCount > 0 &&
                    userBookmarksNovels.loadState.refresh is LoadState.NotLoading &&
                    userBookmarksNovels.loadState.append !is LoadState.NotLoading
                ) {
                    item(key = "append_state") {
                        PagingAppendState(
                            loadStates = userBookmarksNovels.loadState,
                            itemCount = userBookmarksNovels.itemCount,
                            onRetry = userBookmarksNovels::retry,
                            modifier = Modifier.padding(bottom = 136.dp),
                        )
                    }
                }
            }
            CollectionResultsStatus(
                loadStates = userBookmarksNovels.loadState,
                itemCount = userBookmarksNovels.itemCount,
                onRetry = userBookmarksNovels::retry,
                modifier = Modifier.padding(
                    bottom = LocalMainNavigationBottomInset.current +
                            if (userBookmarksNovels.itemCount > 0) 136.dp else 0.dp,
                ),
            )
            VerticalScrollbar(
                state = lazyListState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }

    if (showFilterDialog) {
        CollectionFilterSheet(
            uid = uid,
            query = state.novelQuery,
            collectionViewModel = viewModel,
            onDismiss = { showFilterDialog = false },
            onApply = { query ->
                viewModel.applyFilter(query)
                if (query != state.novelQuery) scope.launch { lazyListState.scrollToItem(0) }
            },
        )
    }
}
