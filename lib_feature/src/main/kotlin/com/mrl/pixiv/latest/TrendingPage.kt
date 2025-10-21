package com.mrl.pixiv.latest

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.mrl.pixiv.common.compose.ui.illust.RectangleIllustItem
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.viewmodel.bookmark.isBookmark
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun TrendingPage(
    modifier: Modifier = Modifier,
    viewModel: LatestViewModel = koinViewModel(),
) {
    val navigationManager = koinInject<NavigationManager>()
    val pullRefreshState = rememberPullToRefreshState()
    val illustsFollowing = viewModel.illustsFollowing.collectAsLazyPagingItems()
    PullToRefreshBox(
        isRefreshing = illustsFollowing.loadState.refresh is LoadState.Loading,
        onRefresh = { illustsFollowing.refresh() },
        modifier = modifier.fillMaxSize(),
        state = pullRefreshState
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 10.dp),
        ) {
            items(
                count = illustsFollowing.itemCount,
                key = illustsFollowing.itemKey { it.id }
            ) { index ->
                val illust = illustsFollowing[index] ?: return@items
                val isBookmarked = illust.isBookmark
                RectangleIllustItem(
                    illust = illust,
                    isBookmarked = isBookmarked,
                    navToPictureScreen = { prefix ->
                        navigationManager.navigateToPictureScreen(
                            illustsFollowing.itemSnapshotList.items,
                            index,
                            prefix
                        )
                    },
                    onBookmarkClick = { restrict: String, tags: List<String>? ->
                        if (isBookmarked) {
                            BookmarkState.deleteBookmarkIllust(illust.id)
                        } else {
                            BookmarkState.bookmarkIllust(illust.id, restrict, tags)
                        }
                    }
                )
            }
        }
    }
}