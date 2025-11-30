package com.mrl.pixiv.common.compose.ui.illust

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.router.NavigateToHorizontalPictureScreen

private const val KEY_LOADING = "loading"

fun LazyGridScope.illustGrid(
    illusts: LazyPagingItems<Illust>,
    navToPictureScreen: NavigateToHorizontalPictureScreen,
    enableLoading: Boolean = false,
) {
    if (enableLoading && illusts.loadState.refresh is LoadState.Loading && illusts.itemCount == 0) {
        item(key = KEY_LOADING, span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator()
            }
        }
    }
    items(
        illusts.itemCount,
        key = { index -> illusts.itemKey { "${index}_${it.id}" }(index) }
    ) { index ->
        val illust = illusts[index] ?: return@items
        val isBookmarked = illust.isBookmark
        SquareIllustItem(
            illust = illust,
            isBookmarked = isBookmarked,
            onBookmarkClick = { restrict, tags, isEdit ->
                if (isEdit || !isBookmarked) {
                    BookmarkState.bookmarkIllust(illust.id, restrict, tags)
                } else {
                    BookmarkState.deleteBookmarkIllust(illust.id)
                }
            },
            navToPictureScreen = { prefix, enableTransition ->
                navToPictureScreen(illusts.itemSnapshotList.items, index, prefix, enableTransition)
            },
            shouldShowTip = index == 0,
        )
    }
}