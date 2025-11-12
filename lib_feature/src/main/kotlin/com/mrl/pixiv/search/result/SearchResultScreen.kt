package com.mrl.pixiv.search.result

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.compose.IllustGridDefaults
import com.mrl.pixiv.common.compose.ui.illust.illustGrid
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.search.result.components.FilterBottomSheet
import com.mrl.pixiv.search.result.components.SearchResultAppBar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun SearchResultsScreen(
    searchWords: String,
    modifier: Modifier = Modifier,
    viewModel: SearchResultViewModel = koinViewModel { parametersOf(searchWords) },
    navigationManager: NavigationManager = koinInject(),
) {
    val state = viewModel.asState()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState()
    val layoutParams = IllustGridDefaults.relatedLayoutParameters()
    val pullRefreshState = rememberPullToRefreshState()
    val isRefreshing = searchResults.loadState.refresh is LoadState.Loading

    Scaffold(
        topBar = {
            SearchResultAppBar(
                searchWords = state.searchWords,
                popBack = navigationManager::popBackStack,
                showBottomSheet = {
                    showBottomSheet = true
                    scope.launch { bottomSheetState.show() }
                }
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { searchResults.refresh() },
            modifier = modifier.padding(it),
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
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
            ) {
                illustGrid(
                    illusts = searchResults,
                    navToPictureScreen = navigationManager::navigateToPictureScreen,
                )
            }
        }

        if (showBottomSheet) {
            FilterBottomSheet(
                bottomSheetState = bottomSheetState,
                searchFilter = state.searchFilter,
                onRefresh = {
                    searchResults.refresh()
                    showBottomSheet = false
                    scope.launch { bottomSheetState.hide() }
                },
                onDismissRequest = {
                    showBottomSheet = false
                    scope.launch { bottomSheetState.hide() }
                },
                onUpdateFilter = {
                    viewModel.dispatch(SearchResultAction.UpdateFilter(it))
                }
            )
        }
    }
}

