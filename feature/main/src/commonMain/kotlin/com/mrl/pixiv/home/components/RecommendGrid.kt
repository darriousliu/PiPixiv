package com.mrl.pixiv.home.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.mrl.pixiv.common.compose.RecommendGridDefaults
import com.mrl.pixiv.common.compose.layout.AdaptiveVerticalStaggeredGrid
import com.mrl.pixiv.common.compose.ui.PagingAppendState
import com.mrl.pixiv.common.compose.ui.illust.RectangleIllustItem
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.kts.itemIndexKey
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.router.NavigateToHorizontalPictureScreen

@Composable
fun RecommendGrid(
    recommendImageList: LazyPagingItems<Illust>,
    navToPictureScreen: NavigateToHorizontalPictureScreen,
    lazyStaggeredGridState: LazyStaggeredGridState,
) {
    val layoutParams = RecommendGridDefaults.coverLayoutParameters()

    AdaptiveVerticalStaggeredGrid(
        state = lazyStaggeredGridState,
        contentPadding = PaddingValues(5.dp),
        columns = layoutParams.gridCells,
        verticalItemSpacing = layoutParams.verticalArrangement.spacing,
        horizontalArrangement = layoutParams.horizontalArrangement,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            count = recommendImageList.itemCount,
            key = recommendImageList.itemIndexKey { index, item -> "${index}_${item.id}" }
        ) {
            val illust = recommendImageList[it] ?: return@items
            val isBookmarked = illust.isBookmark
            RectangleIllustItem(
                navToPictureScreen = { prefix, enableTransition ->
                    navToPictureScreen(
                        recommendImageList.itemSnapshotList.items,
                        it,
                        prefix,
                        enableTransition
                    )
                },
                illust = illust,
                isBookmarked = isBookmarked,
                onBookmarkClick = { restrict, tags, isEdit ->
                    if (isEdit || !isBookmarked) {
                        BookmarkState.bookmarkIllust(illust.id, restrict, tags)
                    } else {
                        BookmarkState.deleteBookmarkIllust(illust.id)
                    }
                },
                enableTransition = true
            )
        }

        if (
            recommendImageList.itemCount > 0 &&
            recommendImageList.loadState.refresh is LoadState.NotLoading &&
            recommendImageList.loadState.append !is LoadState.NotLoading
        ) {
            item(key = "append_state", span = StaggeredGridItemSpan.FullLine) {
                PagingAppendState(
                    loadStates = recommendImageList.loadState,
                    itemCount = recommendImageList.itemCount,
                    onRetry = recommendImageList::retry,
                    modifier = Modifier.padding(bottom = 136.dp),
                )
            }
        }
    }
}
