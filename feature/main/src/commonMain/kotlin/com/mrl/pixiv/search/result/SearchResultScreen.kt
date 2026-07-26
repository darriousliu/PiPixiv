package com.mrl.pixiv.search.result

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.analytics.logEvent
import com.mrl.pixiv.common.compose.IllustGridDefaults
import com.mrl.pixiv.common.compose.listener.KeyEventListener
import com.mrl.pixiv.common.compose.listener.keyboardScrollerController
import com.mrl.pixiv.common.compose.ui.BackToTopButton
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.compose.ui.illust.illustGrid
import com.mrl.pixiv.common.compose.ui.novel.NovelItem
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.setting.SearchResultDisplayMode
import com.mrl.pixiv.common.kts.itemIndexKey
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.paged.PageControls
import com.mrl.pixiv.common.paged.PagedIllustGrid
import com.mrl.pixiv.common.paged.PagedNovelList
import com.mrl.pixiv.common.paged.PagedUserList
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.feed.FeedCapability
import com.mrl.pixiv.common.repository.feed.PagedFeedState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.follow.isFollowing
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.follow.FollowingUserCard
import com.mrl.pixiv.search.result.components.FilterBottomSheet
import com.mrl.pixiv.search.result.components.SearchResultAppBar
import com.mrl.pixiv.strings.illusts
import com.mrl.pixiv.strings.novels
import com.mrl.pixiv.strings.popular_preview_paging_unavailable
import com.mrl.pixiv.strings.switch_to_latest
import com.mrl.pixiv.strings.users
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SearchResultsScreen(
    searchWords: String,
    searchMode: AppViewMode,
    modifier: Modifier = Modifier,
    isIdSearch: Boolean = false,
    viewModel: SearchResultViewModel = koinViewModel {
        parametersOf(
            searchWords,
            searchMode,
            isIdSearch
        )
    },
    navigationManager: NavigationManager = koinInject(),
) {
    val state = viewModel.asState()
    val searchResultDisplayMode by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle {
        searchSettings.searchResultDisplayMode
    }
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val usePagedSearchResults = searchResultDisplayMode == SearchResultDisplayMode.PAGED
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(true)
    val layoutParams = IllustGridDefaults.relatedLayoutParameters()
    val pullRefreshState = rememberPullToRefreshState()
    val novelPullRefreshState = rememberPullToRefreshState()
    val userPullRefreshState = rememberPullToRefreshState()

    val illustsGridState = rememberLazyGridState()
    val novelsListState = rememberLazyListState()
    val usersListState = rememberLazyListState()
    var refreshIllustResults by remember { mutableStateOf<(() -> Unit)?>(null) }
    var refreshNovelResults by remember { mutableStateOf<(() -> Unit)?>(null) }
    var refreshUserResults by remember { mutableStateOf<(() -> Unit)?>(null) }
    var lastConsumedIllustScrollEventId by rememberSaveable { mutableStateOf<Long?>(null) }
    var lastConsumedNovelScrollEventId by rememberSaveable { mutableStateOf<Long?>(null) }
    var lastConsumedUserScrollEventId by rememberSaveable { mutableStateOf<Long?>(null) }

    val pages = remember { SearchResultsPage.entries }
    val pagerState = rememberPagerState { SearchResultsPage.entries.size }
    val page = pages[pagerState.currentPage]
    val scope = rememberCoroutineScope()
    val controller = remember(page, searchMode) {
        when (page) {
            SearchResultsPage.IllustsOrNovel -> {
                when (searchMode) {
                    AppViewMode.ILLUST -> keyboardScrollerController(illustsGridState) {
                        illustsGridState.layoutInfo.viewportSize.height.toFloat()
                    }

                    AppViewMode.NOVEL -> keyboardScrollerController(novelsListState) {
                        novelsListState.layoutInfo.viewportSize.height.toFloat()
                    }
                }
            }

            SearchResultsPage.Users -> keyboardScrollerController(usersListState) {
                usersListState.layoutInfo.viewportSize.height.toFloat()
            }
        }
    }

    @Composable
    fun ManualPagingToolbar(
        state: PagedFeedState<*>,
        page: SearchResultsPage,
        modifier: Modifier = Modifier,
    ) {
        PageControls(
            state = state,
            onPreviousPage = { viewModel.loadPreviousManualPage(page) },
            onNextPage = { viewModel.loadNextManualPage(page) },
            onLoadPage = { viewModel.loadManualPage(page, it) },
            onRetry = { viewModel.refreshManualPage(page) },
            modifier = modifier,
        )
    }

    KeyEventListener(controller)

    LaunchedEffect(pagerState.currentPage) {
        logEvent("screen_view", buildMap {
            put("screen_name", "SearchResults")
            put("page_name", SearchResultsPage.entries[pagerState.currentPage].name)
        })
    }

    Scaffold(
        topBar = {
            Column {
                SearchResultAppBar(
                    searchWords = state.searchWords,
                    bookmarkNumRange = state.bookmarkNumRange,
                    bookmarkStringRange = state.bookmarkStringRange,
                    searchDateRange = state.searchDateRange,
                    onBookmarkNumRangeChanged = {
                        viewModel.dispatch(SearchResultAction.UpdateBookmarkNumRange(it))
                    },
                    onBookmarkStringRangeChanged = {
                        viewModel.updateBookmarkStringRange(it)
                    },
                    onSearchDateRangeChanged = {
                        viewModel.dispatch(SearchResultAction.UpdateSearchDateRange(it))
                    },
                    popBack = navigationManager::popBackStack,
                    showBottomSheet = {
                        showBottomSheet = true
                    },
                    showFilterAction = pagerState.currentPage == 0
                )
                PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        text = {
                            Text(
                                text = when (searchMode) {
                                    AppViewMode.ILLUST -> stringResource(RStrings.illusts)
                                    AppViewMode.NOVEL -> stringResource(RStrings.novels)
                                }
                            )
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text(text = stringResource(RStrings.users)) }
                    )
                }
            }
        },
        floatingActionButton = {
            val currentPage = pages[pagerState.currentPage]
            val onRefresh: () -> Unit = if (usePagedSearchResults) {
                { viewModel.refreshManualPage(currentPage) }
            } else {
                when (currentPage) {
                    SearchResultsPage.IllustsOrNovel -> {
                        when (searchMode) {
                            AppViewMode.ILLUST -> refreshIllustResults ?: {}
                            AppViewMode.NOVEL -> refreshNovelResults ?: {}
                        }
                    }

                    SearchResultsPage.Users -> refreshUserResults ?: {}
                }
            }
            val scrollState = when (currentPage) {
                SearchResultsPage.IllustsOrNovel -> {
                    when (searchMode) {
                        AppViewMode.ILLUST -> illustsGridState
                        AppViewMode.NOVEL -> novelsListState
                    }
                }

                SearchResultsPage.Users -> usersListState
            }
            BackToTopButton(
                visibility = scrollState.canScrollBackward,
                modifier = Modifier,
                onBackToTop = {
                    when (scrollState) {
                        is LazyGridState -> scope.launch { scrollState.scrollToItem(0) }
                        is LazyListState -> scope.launch { scrollState.scrollToItem(0) }
                    }
                },
                onRefresh = onRefresh,
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier.padding(it),
        ) { index ->
            val navigationBarBottomPadding = WindowInsets.navigationBars.asPaddingValues()
                .calculateBottomPadding()

            when (pages[index]) {
                SearchResultsPage.IllustsOrNovel -> {
                    when (searchMode) {
                        AppViewMode.ILLUST -> {
                            val manualIllustResults = if (usePagedSearchResults) {
                                viewModel.manualIllustResults.collectAsStateWithLifecycle().value
                            } else {
                                null
                            }
                            val searchResults = if (usePagedSearchResults) {
                                null
                            } else {
                                viewModel.searchResults.collectAsLazyPagingItems()
                            }

                            if (manualIllustResults != null) {
                                LaunchedEffect(
                                    state.searchFilter,
                                    state.bookmarkNumRange,
                                    state.bookmarkStringRange,
                                    state.searchDateRange,
                                    isPremium,
                                ) {
                                    viewModel.ensureManualPage(
                                        page = SearchResultsPage.IllustsOrNovel,
                                        isPremium = isPremium,
                                    )
                                }
                                ScrollToTopOnPageChange(
                                    eventId = manualIllustResults.scrollToTopEventId,
                                    lastConsumedEventId = lastConsumedIllustScrollEventId,
                                    onEventConsumed = { lastConsumedIllustScrollEventId = it },
                                    onScrollToTop = { illustsGridState.scrollToItem(0) },
                                )
                            }
                            if (searchResults != null) {
                                LaunchedEffect(searchResults) {
                                    refreshIllustResults = { searchResults.refresh() }
                                }
                            }

                            val isRefreshing = manualIllustResults?.isLoading
                                ?: (searchResults?.loadState?.refresh is LoadState.Loading)
                            val showPopularPreviewNotice =
                                manualIllustResults != null &&
                                    viewModel.isPopularPreview(isPremium)
                            val showPagingControls =
                                manualIllustResults?.capability != null &&
                                    manualIllustResults.capability != FeedCapability.SINGLE_PAGE
                            val bottomContentPadding = navigationBarBottomPadding +
                                if (showPagingControls || showPopularPreviewNotice) 88.dp else 0.dp

                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = {
                                    if (manualIllustResults != null) {
                                        viewModel.refreshManualPage(SearchResultsPage.IllustsOrNovel)
                                    } else {
                                        searchResults?.refresh()
                                    }
                                },
                                state = pullRefreshState,
                                indicator = {
                                    PullToRefreshDefaults.LoadingIndicator(
                                        state = pullRefreshState,
                                        isRefreshing = isRefreshing,
                                        modifier = Modifier.align(Alignment.TopCenter),
                                    )
                                },
                            ) {
                                LazyVerticalGrid(
                                    modifier = Modifier.fillMaxSize(),
                                    state = illustsGridState,
                                    columns = layoutParams.gridCells,
                                    verticalArrangement = layoutParams.verticalArrangement,
                                    horizontalArrangement = layoutParams.horizontalArrangement,
                                    contentPadding = PaddingValues(
                                        start = 8.dp,
                                        top = 8.dp,
                                        end = 8.dp,
                                        bottom = bottomContentPadding,
                                    ),
                                ) {
                                    if (manualIllustResults != null) {
                                        PagedIllustGrid(
                                            state = manualIllustResults,
                                            navToPictureScreen = navigationManager::navigateToPictureScreen,
                                        )
                                    } else if (searchResults != null) {
                                        illustGrid(
                                            illusts = searchResults,
                                            navToPictureScreen = navigationManager::navigateToPictureScreen,
                                        )
                                    }
                                }
                                VerticalScrollbar(
                                    state = illustsGridState,
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )
                                if (manualIllustResults != null && showPagingControls) {
                                    ManualPagingToolbar(
                                        state = manualIllustResults,
                                        page = SearchResultsPage.IllustsOrNovel,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .navigationBarsPadding()
                                    )
                                } else if (showPopularPreviewNotice) {
                                    PopularPreviewNotice(
                                        onSwitchToLatest = viewModel::switchToLatestSort,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .navigationBarsPadding(),
                                    )
                                }
                            }
                        }

                        AppViewMode.NOVEL -> {
                            val manualNovelResults = if (usePagedSearchResults) {
                                viewModel.manualNovelResults.collectAsStateWithLifecycle().value
                            } else {
                                null
                            }
                            val novelSearchResults = if (usePagedSearchResults) {
                                null
                            } else {
                                viewModel.novelSearchResults.collectAsLazyPagingItems()
                            }

                            if (manualNovelResults != null) {
                                LaunchedEffect(
                                    state.searchFilter,
                                    state.bookmarkNumRange,
                                    state.bookmarkStringRange,
                                    state.searchDateRange,
                                    isPremium,
                                ) {
                                    viewModel.ensureManualPage(
                                        page = SearchResultsPage.IllustsOrNovel,
                                        isPremium = isPremium,
                                    )
                                }
                                ScrollToTopOnPageChange(
                                    eventId = manualNovelResults.scrollToTopEventId,
                                    lastConsumedEventId = lastConsumedNovelScrollEventId,
                                    onEventConsumed = { lastConsumedNovelScrollEventId = it },
                                    onScrollToTop = { novelsListState.scrollToItem(0) },
                                )
                            }
                            if (novelSearchResults != null) {
                                LaunchedEffect(novelSearchResults) {
                                    refreshNovelResults = { novelSearchResults.refresh() }
                                }
                            }

                            val isNovelRefreshing = manualNovelResults?.isLoading
                                ?: (novelSearchResults?.loadState?.refresh is LoadState.Loading)
                            val showPopularPreviewNotice =
                                manualNovelResults != null &&
                                    viewModel.isPopularPreview(isPremium)
                            val showPagingControls =
                                manualNovelResults?.capability != null &&
                                    manualNovelResults.capability != FeedCapability.SINGLE_PAGE
                            val bottomContentPadding = navigationBarBottomPadding +
                                if (showPagingControls || showPopularPreviewNotice) 88.dp else 0.dp

                            PullToRefreshBox(
                                isRefreshing = isNovelRefreshing,
                                onRefresh = {
                                    if (manualNovelResults != null) {
                                        viewModel.refreshManualPage(SearchResultsPage.IllustsOrNovel)
                                    } else {
                                        novelSearchResults?.refresh()
                                    }
                                },
                                state = novelPullRefreshState,
                                indicator = {
                                    PullToRefreshDefaults.LoadingIndicator(
                                        state = novelPullRefreshState,
                                        isRefreshing = isNovelRefreshing,
                                        modifier = Modifier.align(Alignment.TopCenter),
                                    )
                                },
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    state = novelsListState,
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        top = 10.dp,
                                        end = 16.dp,
                                        bottom = bottomContentPadding,
                                    ),
                                ) {
                                    if (manualNovelResults != null) {
                                        PagedNovelList(
                                            state = manualNovelResults,
                                            onNovelClick = navigationManager::navigateToNovelDetailScreen,
                                        )
                                    } else if (novelSearchResults != null) {
                                        items(
                                            count = novelSearchResults.itemCount,
                                            key = novelSearchResults.itemIndexKey { index, item ->
                                                "${index}_${item.id}"
                                            }
                                        ) { index ->
                                            novelSearchResults[index]?.let { novel ->
                                                NovelItem(
                                                    novel = novel,
                                                    onNovelClick = { novelId ->
                                                        navigationManager.navigateToNovelDetailScreen(
                                                            novelId
                                                        )
                                                    },
                                                    onBookmarkClick = { isBookmarked, restrict, tags ->
                                                        if (isBookmarked) {
                                                            BookmarkState.deleteBookmarkNovel(novel.id)
                                                        } else {
                                                            BookmarkState.bookmarkNovel(
                                                                novel.id,
                                                                restrict,
                                                                tags
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                VerticalScrollbar(
                                    state = novelsListState,
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )
                                if (manualNovelResults != null && showPagingControls) {
                                    ManualPagingToolbar(
                                        state = manualNovelResults,
                                        page = SearchResultsPage.IllustsOrNovel,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .navigationBarsPadding()
                                    )
                                } else if (showPopularPreviewNotice) {
                                    PopularPreviewNotice(
                                        onSwitchToLatest = viewModel::switchToLatestSort,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .navigationBarsPadding(),
                                    )
                                }
                            }
                        }
                    }
                }

                SearchResultsPage.Users -> {
                    val manualUserResults = if (usePagedSearchResults) {
                        viewModel.manualUserResults.collectAsStateWithLifecycle().value
                    } else {
                        null
                    }
                    val userSearchResults = if (usePagedSearchResults) {
                        null
                    } else {
                        viewModel.userSearchResults.collectAsLazyPagingItems()
                    }

                    if (manualUserResults != null) {
                        LaunchedEffect(state.searchWords, isPremium) {
                            viewModel.ensureManualPage(
                                page = SearchResultsPage.Users,
                                isPremium = isPremium,
                            )
                        }
                        ScrollToTopOnPageChange(
                            eventId = manualUserResults.scrollToTopEventId,
                            lastConsumedEventId = lastConsumedUserScrollEventId,
                            onEventConsumed = { lastConsumedUserScrollEventId = it },
                            onScrollToTop = { usersListState.scrollToItem(0) },
                        )
                    }
                    if (userSearchResults != null) {
                        LaunchedEffect(userSearchResults) {
                            refreshUserResults = { userSearchResults.refresh() }
                        }
                    }

                    val isUserRefreshing = manualUserResults?.isLoading
                        ?: (userSearchResults?.loadState?.refresh is LoadState.Loading)
                    val showPagingControls =
                        manualUserResults?.capability != null &&
                            manualUserResults.capability != FeedCapability.SINGLE_PAGE
                    val bottomContentPadding = navigationBarBottomPadding +
                        if (showPagingControls) 88.dp else 0.dp

                    PullToRefreshBox(
                        isRefreshing = isUserRefreshing,
                        onRefresh = {
                            if (manualUserResults != null) {
                                viewModel.refreshManualPage(SearchResultsPage.Users)
                            } else {
                                userSearchResults?.refresh()
                            }
                        },
                        state = userPullRefreshState,
                        indicator = {
                            PullToRefreshDefaults.LoadingIndicator(
                                state = userPullRefreshState,
                                isRefreshing = isUserRefreshing,
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        },
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = usersListState,
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = 10.dp,
                                end = 16.dp,
                                bottom = bottomContentPadding,
                            ),
                            verticalArrangement = 10f.spaceBy,
                        ) {
                            if (manualUserResults != null) {
                                PagedUserList(
                                    state = manualUserResults,
                                    navToPictureScreen = navigationManager::navigateToPictureScreen,
                                    navToUserProfile = navigationManager::navigateToProfileDetailScreen,
                                    showIllusts = searchMode == AppViewMode.ILLUST,
                                )
                            } else if (userSearchResults != null) {
                                items(
                                    count = userSearchResults.itemCount,
                                    key = userSearchResults.itemIndexKey { index, item ->
                                        "${index}_${item.user.id}"
                                    }
                                ) { index ->
                                    val userPreview = userSearchResults[index] ?: return@items
                                    FollowingUserCard(
                                        illusts = when (searchMode) {
                                            AppViewMode.ILLUST -> userPreview.illusts.toImmutableList()
                                            AppViewMode.NOVEL -> persistentListOf()
                                        },
                                        userName = userPreview.user.name,
                                        userId = userPreview.user.id,
                                        userAvatar = userPreview.user.profileImageUrls.medium,
                                        isFollowed = userPreview.user.isFollowing,
                                        navToPictureScreen = navigationManager::navigateToPictureScreen,
                                        navToUserProfile = {
                                            navigationManager.navigateToProfileDetailScreen(userPreview.user.id)
                                        }
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(
                            state = usersListState,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                        if (manualUserResults != null && showPagingControls) {
                            ManualPagingToolbar(
                                state = manualUserResults,
                                page = SearchResultsPage.Users,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding(),
                            )
                        }
                    }
                }
            }
        }

        if (showBottomSheet) {
            FilterBottomSheet(
                bottomSheetState = bottomSheetState,
                searchFilter = state.searchFilter,
                onDismissRequest = {
                    showBottomSheet = false
                },
                onUpdateFilter = {
                    viewModel.dispatch(SearchResultAction.UpdateFilter(it))
                },
                isNovelMode = searchMode == AppViewMode.NOVEL
            )
        }
    }
}

@Composable
private fun ScrollToTopOnPageChange(
    eventId: Long,
    lastConsumedEventId: Long?,
    onEventConsumed: (Long) -> Unit,
    onScrollToTop: suspend () -> Unit,
) {
    LaunchedEffect(eventId, lastConsumedEventId) {
        when {
            lastConsumedEventId == null || eventId == 0L -> onEventConsumed(eventId)
            eventId != lastConsumedEventId -> {
                onScrollToTop()
                onEventConsumed(eventId)
            }
        }
    }
}

@Composable
private fun PopularPreviewNotice(
    onSwitchToLatest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(RStrings.popular_preview_paging_unavailable),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onSwitchToLatest) {
                Text(text = stringResource(RStrings.switch_to_latest))
            }
        }
    }
}
