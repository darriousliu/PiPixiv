package com.mrl.pixiv.home.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.mrl.pixiv.common.compose.RecommendGridDefaults
import com.mrl.pixiv.common.compose.ui.illust.RectangleIllustItem
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.router.NavigateToHorizontalPictureScreen
import com.mrl.pixiv.common.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.viewmodel.bookmark.isBookmark

@Composable
fun RecommendGrid(
    recommendImageList: LazyPagingItems<Illust>,
    navToPictureScreen: NavigateToHorizontalPictureScreen,
    lazyStaggeredGridState: LazyStaggeredGridState,
) {
    val layoutParams = RecommendGridDefaults.coverLayoutParameters()

    LazyVerticalStaggeredGrid(
        state = lazyStaggeredGridState,
        contentPadding = PaddingValues(5.dp),
        columns = layoutParams.gridCells,
        verticalItemSpacing = layoutParams.verticalArrangement.spacing,
        horizontalArrangement = layoutParams.horizontalArrangement,
        modifier = Modifier.fillMaxSize()
    ) {
        items(recommendImageList.itemCount, key = recommendImageList.itemKey { it.id }) {
            val illust = recommendImageList[it] ?: return@items
            val isBookmarked = illust.isBookmark
            RectangleIllustItem(
                navToPictureScreen = { prefix ->
                    navToPictureScreen(recommendImageList.itemSnapshotList.items, it, prefix)
                },
                illust = illust,
                isBookmarked = isBookmarked,
                onBookmarkClick = { restrict, tags ->
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