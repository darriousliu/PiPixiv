package com.mrl.pixiv.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.compose.listener.KeyEventListener
import com.mrl.pixiv.common.compose.listener.keyboardScrollerController
import com.mrl.pixiv.common.compose.ui.BackToTopButton
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.compose.ui.novel.NovelItem
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.kts.VSpacer
import com.mrl.pixiv.common.kts.itemIndexKey
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.router.NavigateToHorizontalPictureScreen
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.home.components.RecommendGrid
import com.mrl.pixiv.main.components.ViewModeToggleButton
import com.mrl.pixiv.strings.app_name
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
    navigationManager: NavigationManager = koinInject(),
) {
    val lazyStaggeredGridState = viewModel.lazyStaggeredGridState
    val lazyListState = viewModel.lazyListState
    val scope = rememberCoroutineScope()
    val onRefresh = viewModel::refresh
    val appViewMode by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle { appViewMode }
    val scrollToTop = suspend {
        when (appViewMode) {
            AppViewMode.ILLUST -> lazyStaggeredGridState.scrollToItem(0)
            AppViewMode.NOVEL -> lazyListState.scrollToItem(0)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(RStrings.app_name)) },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                scrollToTop()
                            }
                            onRefresh()
                        },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            Column {
                BackToTopButton(
                    visibility = when (appViewMode) {
                        AppViewMode.ILLUST -> lazyStaggeredGridState.canScrollBackward
                        AppViewMode.NOVEL -> lazyListState.canScrollBackward
                    },
                    modifier = Modifier,
                    onBackToTop = scrollToTop,
                    onRefresh = onRefresh
                )
                8.VSpacer
                ViewModeToggleButton(
                    currentMode = appViewMode,
                    onModeChange = viewModel::switchViewMode
                )
            }
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
    ) { paddingValues ->
        when (appViewMode) {
            AppViewMode.ILLUST -> {
                IllustMode(
                    navigateToPictureScreen = navigationManager::navigateToPictureScreen,
                    modifier = Modifier.padding(paddingValues),
                    viewModel = viewModel,
                )
            }

            AppViewMode.NOVEL -> {
                NovelMode(
                    navigateToNovelDetailScreen = navigationManager::navigateToNovelDetailScreen,
                    modifier = Modifier.padding(paddingValues),
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun IllustMode(
    navigateToPictureScreen: NavigateToHorizontalPictureScreen,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val recommendImageList = viewModel.recommendImageList.collectAsLazyPagingItems()
    val lazyStaggeredGridState = viewModel.lazyStaggeredGridState
    val pullRefreshState = rememberPullToRefreshState()
    val onRefresh = recommendImageList::refresh
    val isRefreshing = recommendImageList.loadState.refresh is LoadState.Loading
    val controller = remember {
        keyboardScrollerController(lazyStaggeredGridState) {
            lazyStaggeredGridState.layoutInfo.viewportSize.height.toFloat()
        }
    }

    KeyEventListener(controller)

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is HomeSideEffect.Refresh -> recommendImageList.refresh()
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = pullRefreshState,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            RecommendGrid(
                recommendImageList = recommendImageList,
                navToPictureScreen = navigateToPictureScreen,
                lazyStaggeredGridState = lazyStaggeredGridState,
            )
            VerticalScrollbar(
                state = lazyStaggeredGridState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun NovelMode(
    navigateToNovelDetailScreen: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val recommendNovelList = viewModel.recommendNovelList.collectAsLazyPagingItems()
    val lazyListState = viewModel.lazyListState
    val pullRefreshState = rememberPullToRefreshState()
    val onRefresh = recommendNovelList::refresh
    val isRefreshing = recommendNovelList.loadState.refresh is LoadState.Loading
    val controller = remember {
        keyboardScrollerController(lazyListState) {
            lazyListState.layoutInfo.viewportSize.height.toFloat()
        }
    }

    KeyEventListener(controller)

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is HomeSideEffect.Refresh -> recommendNovelList.refresh()
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = pullRefreshState,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
        ) {
            items(
                count = recommendNovelList.itemCount,
                key = recommendNovelList.itemIndexKey { index, item -> "${index}_${item.id}" }
            ) { index ->
                recommendNovelList[index]?.let { novel ->
                    NovelItem(
                        novel = novel,
                        isBookmarked = novel.isBookmarked,
                        onNovelClick = { novelId ->
                            navigateToNovelDetailScreen(novelId)
                        },
                        onBookmarkClick = { restrict, tags ->
                            if (novel.isBookmarked) {
                                BookmarkState.deleteBookmarkNovel(novel.id)
                            } else {
                                BookmarkState.bookmarkNovel(novel.id, restrict, tags)
                            }
                        }
                    )
                }
            }
        }
    }
}