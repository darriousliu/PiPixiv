package com.mrl.pixiv.search.result

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.analytics.logEvent
import com.mrl.pixiv.common.compose.IllustGridDefaults
import com.mrl.pixiv.common.compose.ui.illust.illustGrid
import com.mrl.pixiv.common.kts.itemIndexKey
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.viewmodel.follow.isFollowing
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.follow.FollowingUserCard
import com.mrl.pixiv.search.result.components.FilterBottomSheet
import com.mrl.pixiv.search.result.components.SearchResultAppBar
import com.mrl.pixiv.strings.illusts
import com.mrl.pixiv.strings.users
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SearchResultsScreen(
    searchWords: String,
    isIdSearch: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: SearchResultViewModel = koinViewModel { parametersOf(searchWords, isIdSearch) },
    navigationManager: NavigationManager = koinInject(),
) {
    val state = viewModel.asState()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    val userSearchResults = viewModel.userSearchResults.collectAsLazyPagingItems()
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(true)
    val layoutParams = IllustGridDefaults.relatedLayoutParameters()
    val pullRefreshState = rememberPullToRefreshState()
    val userPullRefreshState = rememberPullToRefreshState()
    val isRefreshing = searchResults.loadState.refresh is LoadState.Loading
    val isUserRefreshing = userSearchResults.loadState.refresh is LoadState.Loading

    val pages = remember { SearchResultsPage.entries }
    val pagerState = rememberPagerState { SearchResultsPage.entries.size }
    val scope = rememberCoroutineScope()

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
                    searchDateRange = state.searchDateRange,
                    onBookmarkNumRangeChanged = {
                        viewModel.dispatch(
                            SearchResultAction.UpdateBookmarkNumRange(
                                it
                            )
                        )
                    },
                    onSearchDateRangeChanged = {
                        viewModel.dispatch(
                            SearchResultAction.UpdateSearchDateRange(
                                it
                            )
                        )
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
                        text = { Text(text = stringResource(RStrings.illusts)) }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text(text = stringResource(RStrings.users)) }
                    )
                }
            }
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier.padding(it),
        ) { index ->
            when (pages[index]) {
                SearchResultsPage.Illusts -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { searchResults.refresh() },
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
                            columns = layoutParams.gridCells,
                            verticalArrangement = layoutParams.verticalArrangement,
                            horizontalArrangement = layoutParams.horizontalArrangement,
                            contentPadding = PaddingValues(
                                start = 8.dp,
                                top = 8.dp,
                                end = 8.dp,
                                bottom = WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding()
                            ),
                        ) {
                            illustGrid(
                                illusts = searchResults,
                                navToPictureScreen = navigationManager::navigateToPictureScreen,
                            )
                        }
                    }
                }

                SearchResultsPage.Users -> {
                    PullToRefreshBox(
                        isRefreshing = isUserRefreshing,
                        onRefresh = { userSearchResults.refresh() },
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
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = 10.dp,
                                end = 16.dp,
                                bottom = WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding()
                            ),
                            verticalArrangement = 10f.spaceBy,
                        ) {
                            items(
                                count = userSearchResults.itemCount,
                                key = userSearchResults.itemIndexKey { index, item -> "${index}_${item.user.id}" }
                            ) { index ->
                                val userPreview = userSearchResults[index] ?: return@items
                                FollowingUserCard(
                                    illusts = userPreview.illusts.toImmutableList(),
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
                }
            )
        }
    }
}
