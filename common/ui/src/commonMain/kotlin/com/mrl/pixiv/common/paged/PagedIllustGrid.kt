package com.mrl.pixiv.common.paged

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import com.mrl.pixiv.common.compose.ui.illust.RectangleIllustItem
import com.mrl.pixiv.common.compose.ui.illust.SquareIllustItem
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.repository.feed.PagedFeedState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.router.NavigateToHorizontalPictureScreen

fun LazyGridScope.PagedIllustGrid(
    state: PagedFeedState<Illust>,
    navToPictureScreen: NavigateToHorizontalPictureScreen,
) {
    items(
        count = state.items.size,
        key = { index -> "${index}_${state.items[index].id}" },
    ) { index ->
        val illust = state.items[index]
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
                navToPictureScreen(state.items, index, prefix, enableTransition)
            },
            shouldShowTip = index == 0,
        )
    }
}

fun LazyStaggeredGridScope.PagedIllustGrid(
    state: PagedFeedState<Illust>,
    navToPictureScreen: NavigateToHorizontalPictureScreen,
) {
    items(
        count = state.items.size,
        key = { index -> "${index}_${state.items[index].id}" },
    ) { index ->
        val illust = state.items[index]
        val isBookmarked = illust.isBookmark
        RectangleIllustItem(
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
                navToPictureScreen(state.items, index, prefix, enableTransition)
            },
            shouldShowTip = index == 0,
        )
    }
}
