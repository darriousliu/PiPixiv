package com.mrl.pixiv.latest

import androidx.compose.foundation.layout.PaddingValues
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
import com.mrl.pixiv.collection.CollectionViewModel
import com.mrl.pixiv.common.compose.ui.illust.RectangleIllustItem
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.viewmodel.bookmark.isBookmark
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun CollectionPage(
    uid: Long,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = koinViewModel { parametersOf(uid) },
    navigationManager: NavigationManager = koinInject(),
) {
    val userBookmarksIllusts = viewModel.userBookmarksIllusts.collectAsLazyPagingItems()
    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = userBookmarksIllusts.loadState.refresh is LoadState.Loading,
        onRefresh = { userBookmarksIllusts.refresh() },
        modifier = modifier,
        state = pullRefreshState
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 10.dp),
        ) {
            items(
                count = userBookmarksIllusts.itemCount,
                key = userBookmarksIllusts.itemKey { it.id }
            ) { index ->
                val illust = userBookmarksIllusts[index] ?: return@items
                val isBookmarked = illust.isBookmark
                RectangleIllustItem(
                    illust = illust,
                    isBookmarked = isBookmarked,
                    navToPictureScreen = { prefix ->
                        navigationManager.navigateToPictureScreen(
                            userBookmarksIllusts.itemSnapshotList.items,
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
