package com.mrl.pixiv.common.repository.viewmodel.bookmark

import androidx.compose.runtime.mutableStateMapOf
import com.mrl.pixiv.common.coroutine.launchProcess
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.repository.PixivRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

data class BookmarkInfo(
    val isBookmarked: Boolean,
    val restrict: Restrict? = null,
) {
    val isPrivate: Boolean
        get() = isBookmarked && restrict == Restrict.PRIVATE
}

val Illust.bookmarkInfo: BookmarkInfo
    get() = BookmarkState.illustState[id] ?: BookmarkInfo(isBookmarked)

val Illust.isBookmark: Boolean
    get() = bookmarkInfo.isBookmarked

val Illust.isPrivateBookmark: Boolean
    get() = bookmarkInfo.isPrivate

val Novel.bookmarkInfo: BookmarkInfo
    get() = BookmarkState.novelState[id] ?: BookmarkInfo(isBookmarked)

val Novel.isBookmark: Boolean
    get() = bookmarkInfo.isBookmarked

val Novel.isPrivateBookmark: Boolean
    get() = bookmarkInfo.isPrivate

object BookmarkState {
    internal val illustState = mutableStateMapOf<Long, BookmarkInfo>()

    internal val novelState = mutableStateMapOf<Long, BookmarkInfo>()

    fun updateIllustBookmarkDetail(
        illustId: Long,
        isBookmarked: Boolean,
        restrict: Restrict,
    ) {
        illustState[illustId] = BookmarkInfo(isBookmarked, restrict)
    }

    fun updateNovelBookmarkDetail(
        novelId: Long,
        isBookmarked: Boolean,
        restrict: Restrict,
    ) {
        novelState[novelId] = BookmarkInfo(isBookmarked, restrict)
    }

    fun bookmarkIllust(
        illustId: Long,
        restrict: Restrict = Restrict.PUBLIC,
        tags: List<String>? = null
    ) {
        launchProcess(Dispatchers.IO) {
            PixivRepository.postIllustBookmarkAdd(illustId, restrict, tags)
            illustState[illustId] = BookmarkInfo(true, restrict)
        }
    }

    fun deleteBookmarkIllust(illustId: Long) {
        launchProcess(Dispatchers.IO) {
            PixivRepository.postIllustBookmarkDelete(illustId)
            illustState[illustId] = BookmarkInfo(false)
        }
    }

    fun bookmarkNovel(
        id: Long,
        restrict: Restrict = Restrict.PUBLIC,
        tags: List<String>? = null
    ) {
        launchProcess(Dispatchers.IO) {
            PixivRepository.postNovelBookmarkAdd(id, restrict, tags)
            novelState[id] = BookmarkInfo(true, restrict)
        }
    }

    fun deleteBookmarkNovel(id: Long) {
        launchProcess(Dispatchers.IO) {
            PixivRepository.postNovelBookmarkDelete(id)
            novelState[id] = BookmarkInfo(false)
        }
    }
}
