package com.mrl.pixiv.collection

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.collection.components.FilterDialog
import com.mrl.pixiv.common.compose.IllustGridDefaults
import com.mrl.pixiv.common.compose.ui.illust.illustGrid
import com.mrl.pixiv.common.datasource.local.mmkv.isSelf
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.viewmodel.asState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun CollectionScreen(
    uid: Long,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = koinViewModel { parametersOf(uid) },
    navigationManager: NavigationManager = koinInject()
) {
    val state = viewModel.asState()
    val userBookmarksIllusts = viewModel.userBookmarksIllusts.collectAsLazyPagingItems()
    val dispatch = viewModel::dispatch
    var showFilterDialog by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            CollectionTopAppBar(
                uid = uid,
                showFilterDialog = { showFilterDialog = true },
                onBack = { navigationManager.popBackStack() }
            )
        },
        contentWindowInsets = WindowInsets.statusBars
    ) {
        val layoutParams = IllustGridDefaults.relatedLayoutParameters()
        val lazyGridState = rememberLazyGridState()
        val pullRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
            isRefreshing = userBookmarksIllusts.loadState.refresh is LoadState.Loading,
            onRefresh = { userBookmarksIllusts.refresh() },
            modifier = Modifier.padding(it),
            state = pullRefreshState
        ) {
            LazyVerticalGrid(
                state = lazyGridState,
                modifier = Modifier.fillMaxSize(),
                columns = layoutParams.gridCells,
                verticalArrangement = layoutParams.verticalArrangement,
                horizontalArrangement = layoutParams.horizontalArrangement,
                contentPadding = PaddingValues(
                    start = 8.dp,
                    top = 10.dp,
                    end = 8.dp,
                    bottom = 20.dp
                ),
            ) {
                illustGrid(
                    illusts = userBookmarksIllusts,
                    navToPictureScreen = navigationManager::navigateToPictureScreen,
                )
            }
        }
        if (showFilterDialog) {
            FilterDialog(
                onDismissRequest = { showFilterDialog = false },
                userBookmarkTagsIllust = state.userBookmarkTagsIllust,
                privateBookmarkTagsIllust = state.privateBookmarkTagsIllust,
                restrict = state.restrict,
                filterTag = state.filterTag,
                onLoadUserBookmarksTags = {
                    dispatch(CollectionAction.LoadUserBookmarksTagsIllust(it))
                },
                onSelected = { restrict: String, tag: String? ->
                    viewModel.updateFilterTag(restrict, tag)
                    userBookmarksIllusts.refresh()
                }
            )
        }
    }
}

@Composable
private fun CollectionTopAppBar(
    uid: Long,
    showFilterDialog: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    TopAppBar(
        modifier = Modifier.shadow(4.dp),
        title = {
            Text(text = stringResource(RString.collection))
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            if (uid.isSelf) {
                IconButton(onClick = showFilterDialog) {
                    Icon(Icons.Rounded.FilterList, contentDescription = null)
                }
            }
        }
    )
}