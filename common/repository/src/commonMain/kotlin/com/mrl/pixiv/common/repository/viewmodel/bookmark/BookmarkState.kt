package com.mrl.pixiv.common.repository.viewmodel.bookmark

import androidx.compose.runtime.mutableStateMapOf
import com.mrl.pixiv.common.coroutine.launchProcess
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.repository.PixivRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

val Illust.isBookmark: Boolean
    get() = BookmarkState.illustState[id] ?: isBookmarked

val Novel.isBookmark: Boolean
    get() = BookmarkState.novelState[id] ?: isBookmarked


object BookmarkState {
    internal val illustState = mutableStateMapOf<Long, Boolean>()

    internal val novelState = mutableStateMapOf<Long, Boolean>()

    fun bookmarkIllust(
        illustId: Long,
        restrict: Restrict = Restrict.PUBLIC,
        tags: List<String>? = null
    ) {
        launchProcess(Dispatchers.IO) {
            PixivRepository.postIllustBookmarkAdd(illustId, restrict, tags)
            illustState[illustId] = true
        }
    }

    fun deleteBookmarkIllust(illustId: Long) {
        launchProcess(Dispatchers.IO) {
            PixivRepository.postIllustBookmarkDelete(illustId)
            illustState[illustId] = false
        }
    }

    fun bookmarkNovel(
        id: Long,
        restrict: Restrict = Restrict.PUBLIC,
        tags: List<String>? = null
    ) {
        launchProcess(Dispatchers.IO) {
            PixivRepository.postNovelBookmarkAdd(id, restrict, tags)
            novelState[id] = true
        }
    }

    fun deleteBookmarkNovel(id: Long) {
        launchProcess(Dispatchers.IO) {
            PixivRepository.postNovelBookmarkDelete(id)
            novelState[id] = false
        }
    }
}