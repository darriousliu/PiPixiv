package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.comment.Comment
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.util.filterBlocked
import com.mrl.pixiv.common.repository.util.queryParams
import com.mrl.pixiv.common.router.CommentType

class CommentPagingSource(
    private val id: Long,
    private val type: CommentType
) : PagingSource<String, Comment>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Comment> {
        return try {
            val resp = if (params.key.isNullOrEmpty()) {
                when (type) {
                    CommentType.ILLUST -> PixivRepository.getIllustComments(id)
                    CommentType.NOVEL -> PixivRepository.getNovelComments(id)
                }
            } else {
                when (type) {
                    CommentType.ILLUST -> PixivRepository.loadMoreIllustComments(params.key!!.queryParams)
                    CommentType.NOVEL -> PixivRepository.loadMoreNovelComments(params.key!!.queryParams)
                }
            }
            LoadResult.Page(
                data = resp.comments.filterBlocked(),
                prevKey = params.key,
                nextKey = resp.nextUrl?.takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Comment>): String? {
        return null
    }
}