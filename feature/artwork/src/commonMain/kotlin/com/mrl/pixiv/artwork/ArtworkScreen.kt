package com.mrl.pixiv.artwork

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.compose.IllustGridDefaults
import com.mrl.pixiv.common.compose.listener.KeyEventListener
import com.mrl.pixiv.common.compose.listener.keyboardScrollerController
import com.mrl.pixiv.common.compose.ui.BackToTopButton
import com.mrl.pixiv.common.compose.ui.ContentPullToRefreshBox
import com.mrl.pixiv.common.compose.ui.PagingAppendState
import com.mrl.pixiv.common.compose.ui.PagingContentState
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.compose.ui.illust.illustGrid
import com.mrl.pixiv.common.compose.ui.novel.NovelItem
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.kts.itemIndexKey
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.artworks
import com.mrl.pixiv.strings.illustrations
import com.mrl.pixiv.strings.manga
import com.mrl.pixiv.strings.novels
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Stable
private enum class ArtworkPage(
    val type: Type?,
    val title: org.jetbrains.compose.resources.StringResource,
) {
    Illust(Type.Illust, RStrings.illustrations),
    Novel(null, RStrings.novels),
    Manga(Type.Manga, RStrings.manga);
}

internal fun resolveArtworkInitialPage(
    initialType: Type,
    initialNovel: Boolean,
): Int = when {
    initialNovel -> 1
    initialType == Type.Manga -> 2
    else -> 0
}

@Composable
fun ArtworkScreen(
    userId: Long,
    initialType: Type = Type.Illust,
    modifier: Modifier = Modifier,
    viewModel: ArtworkViewModel = koinViewModel { parametersOf(userId) },
    navigationManager: NavigationManager = currentNavigationManager(),
    initialNovel: Boolean = false,
) {
    val userIllusts = viewModel.userIllusts.collectAsLazyPagingItems()
    val userNovels = viewModel.userNovels.collectAsLazyPagingItems()
    val userMangas = viewModel.userMangas.collectAsLazyPagingItems()
    val pages = remember { ArtworkPage.entries }
    val initialPage = remember(initialType, initialNovel) {
        resolveArtworkInitialPage(initialType, initialNovel)
    }
    val pagerState = rememberPagerState(initialPage = initialPage) { pages.size }
    val scope = rememberCoroutineScope()
    val illustGridState = rememberLazyGridState()
    val novelListState = rememberLazyListState()
    val mangaGridState = rememberLazyGridState()
    val controller = remember(pagerState.currentPage) {
        when (pages[pagerState.currentPage]) {
            ArtworkPage.Illust -> keyboardScrollerController(illustGridState) {
                illustGridState.layoutInfo.viewportSize.height.toFloat()
            }

            ArtworkPage.Novel -> keyboardScrollerController(novelListState) {
                novelListState.layoutInfo.viewportSize.height.toFloat()
            }

            ArtworkPage.Manga -> keyboardScrollerController(mangaGridState) {
                mangaGridState.layoutInfo.viewportSize.height.toFloat()
            }
        }
    }

    KeyEventListener(controller)

    Scaffold(
        modifier = modifier,
        topBar = {
            CollectionTopAppBar(onBack = navigationManager::popBackStack)
        },
        floatingActionButton = {
            val canScrollBackward = when (pages[pagerState.currentPage]) {
                ArtworkPage.Illust -> illustGridState.canScrollBackward
                ArtworkPage.Novel -> novelListState.canScrollBackward
                ArtworkPage.Manga -> mangaGridState.canScrollBackward
            }
            BackToTopButton(
                visibility = canScrollBackward,
                modifier = Modifier,
                onBackToTop = {
                    when (pages[pagerState.currentPage]) {
                        ArtworkPage.Illust -> illustGridState.scrollToItem(0)
                        ArtworkPage.Novel -> novelListState.scrollToItem(0)
                        ArtworkPage.Manga -> mangaGridState.scrollToItem(0)
                    }
                },
                onRefresh = {
                    when (pages[pagerState.currentPage]) {
                        ArtworkPage.Illust -> userIllusts.refresh()
                        ArtworkPage.Novel -> userNovels.refresh()
                        ArtworkPage.Manga -> userMangas.refresh()
                    }
                }
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                pages.forEachIndexed { index, page ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(text = stringResource(page.title)) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (pages[page]) {
                    ArtworkPage.Illust -> UserIllustPage(
                        illusts = userIllusts,
                        gridState = illustGridState,
                        navToPictureScreen = navigationManager::navigateToPictureScreen,
                    )

                    ArtworkPage.Novel -> UserNovelPage(
                        novels = userNovels,
                        listState = novelListState,
                        navToNovelDetailScreen = navigationManager::navigateToNovelDetailScreen,
                        navToNovelSeriesScreen = navigationManager::navigateToNovelSeriesScreen,
                    )

                    ArtworkPage.Manga -> UserIllustPage(
                        illusts = userMangas,
                        gridState = mangaGridState,
                        navToPictureScreen = navigationManager::navigateToPictureScreen,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserIllustPage(
    illusts: LazyPagingItems<Illust>,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    navToPictureScreen: com.mrl.pixiv.common.router.NavigateToHorizontalPictureScreen,
) {
    val layoutParams = IllustGridDefaults.relatedLayoutParameters()
    val pullRefreshState = rememberPullToRefreshState()
    val isRefreshing = illusts.loadState.refresh is LoadState.Loading

    ContentPullToRefreshBox(
        isRefreshing = isRefreshing,
        hasContent = illusts.itemCount > 0,
        onRefresh = illusts::refresh,
        state = pullRefreshState,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            columns = layoutParams.gridCells,
            verticalArrangement = layoutParams.verticalArrangement,
            horizontalArrangement = layoutParams.horizontalArrangement,
            contentPadding = PaddingValues(
                start = 8.dp,
                top = 8.dp,
                end = 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
        ) {
            illustGrid(
                illusts = illusts,
                navToPictureScreen = navToPictureScreen,
                enableLoading = false,
            )
            if (
                illusts.itemCount > 0 &&
                illusts.loadState.refresh is LoadState.NotLoading &&
                illusts.loadState.append !is LoadState.NotLoading
            ) {
                item(key = "append_state", span = { GridItemSpan(maxLineSpan) }) {
                    PagingAppendState(illusts.loadState, illusts.itemCount, illusts::retry)
                }
            }
        }
        PagingContentState(
            loadStates = illusts.loadState,
            itemCount = illusts.itemCount,
            onRetry = illusts::retry,
            modifier = Modifier.fillMaxSize().padding(
                bottom = if (illusts.itemCount > 0) {
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 88.dp
                } else {
                    0.dp
                },
            ),
        )
        VerticalScrollbar(
            state = gridState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun UserNovelPage(
    novels: LazyPagingItems<Novel>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    navToNovelDetailScreen: (Long) -> Unit,
    navToNovelSeriesScreen: (Long) -> Unit,
) {
    val pullRefreshState = rememberPullToRefreshState()
    val isRefreshing = novels.loadState.refresh is LoadState.Loading

    ContentPullToRefreshBox(
        isRefreshing = isRefreshing,
        hasContent = novels.itemCount > 0,
        onRefresh = novels::refresh,
        state = pullRefreshState,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
        ) {
            items(
                count = novels.itemCount,
                key = novels.itemIndexKey { index, item -> "${index}_${item.id}" }
            ) { index ->
                novels[index]?.let { novel ->
                    NovelItem(
                        novel = novel,
                        onNovelClick = navToNovelDetailScreen,
                        onSeriesClick = navToNovelSeriesScreen,
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
                novels.itemCount > 0 &&
                novels.loadState.refresh is LoadState.NotLoading &&
                novels.loadState.append !is LoadState.NotLoading
            ) {
                item(key = "append_state") {
                    PagingAppendState(novels.loadState, novels.itemCount, novels::retry)
                }
            }
        }
        PagingContentState(
            loadStates = novels.loadState,
            itemCount = novels.itemCount,
            onRetry = novels::retry,
            modifier = Modifier.fillMaxSize().padding(
                bottom = if (novels.itemCount > 0) {
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 88.dp
                } else {
                    0.dp
                },
            ),
        )
        VerticalScrollbar(
            state = listState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun CollectionTopAppBar(
    onBack: () -> Unit = {},
) {
    TopAppBar(
        modifier = Modifier.shadow(4.dp),
        title = {
            Text(text = stringResource(RStrings.artworks))
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
        },
    )
}
