package com.mrl.pixiv.search.result

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
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
    val spanCount = when (LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> 2
        Configuration.ORIENTATION_LANDSCAPE -> 4
        else -> 2
    }
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
        }
    ) {
        PullToRefreshBox(
            isRefreshing = searchResults.loadState.refresh is LoadState.Loading,
            onRefresh = { searchResults.refresh() },
            modifier = modifier.padding(it),
        ) {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Fixed(spanCount),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 20.dp),
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

