package com.mrl.pixiv.comment

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.comment.Comment
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.util.queryParams

class CommentPagingSource(
    private val illustId: Long
) : PagingSource<String, Comment>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Comment> {
        return try {
            val resp = if (params.key.isNullOrEmpty()) {
                PixivRepository.getIllustComments(illustId)
            } else {
                PixivRepository.loadMoreIllustComments(params.key!!.queryParams)
            }
            LoadResult.Page(
                data = resp.comments,
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
