package com.mrl.pixiv.common.paged

import androidx.compose.foundation.lazy.LazyListScope
import com.mrl.pixiv.common.compose.ui.novel.NovelItem
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.repository.feed.PagedFeedState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState

fun LazyListScope.PagedNovelList(
    state: PagedFeedState<Novel>,
    onNovelClick: (Long) -> Unit,
    onSeriesClick: (Long) -> Unit,
    compactTitle: Boolean = false,
) {
    items(
        count = state.items.size,
        key = { index -> "${index}_${state.items[index].id}" },
    ) { index ->
        val novel = state.items[index]
        NovelItem(
            novel = novel,
            onNovelClick = onNovelClick,
            onSeriesClick = onSeriesClick,
            compactTitle = compactTitle,
            onBookmarkClick = { isBookmarked, restrict, tags ->
                if (isBookmarked) {
                    BookmarkState.deleteBookmarkNovel(novel.id)
                } else {
                    BookmarkState.bookmarkNovel(novel.id, restrict, tags)
                }
            }
        )
    }
}
