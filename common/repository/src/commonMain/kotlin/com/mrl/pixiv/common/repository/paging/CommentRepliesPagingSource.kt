package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.comment.Comment
import com.mrl.pixiv.common.datasource.remote.PixivApi
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.util.filterBlocked
import com.mrl.pixiv.common.repository.util.queryParams
import com.mrl.pixiv.common.router.CommentType
import kotlinx.coroutines.CancellationException

class CommentRepliesPagingSource internal constructor(
    private val commentId: Long,
    private val type: CommentType,
    private val api: PixivApi?,
    private val filterComments: (List<Comment>) -> List<Comment> = { it.filterBlocked() },
) : PagingSource<String, Comment>() {
    constructor(commentId: Long, type: CommentType) : this(commentId, type, null)

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Comment> {
        return try {
            val resp = api?.let { loadReplies(it, params.key) }
                ?: PixivRepository.withApi { loadReplies(it, params.key) }
            LoadResult.Page(
                data = filterComments(resp.comments),
                prevKey = null,
                nextKey = resp.nextUrl?.takeIf { it.isNotBlank() }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private suspend fun loadReplies(api: PixivApi, key: String?) = if (key.isNullOrEmpty()) {
        when (type) {
            CommentType.ILLUST -> api.getIllustCommentReplies(commentId)
            CommentType.NOVEL -> api.getNovelCommentReplies(commentId)
        }
    } else {
        val query = key.orEmpty().queryParams
        when (type) {
            CommentType.ILLUST -> api.loadMoreIllustCommentReplies(query)
            CommentType.NOVEL -> api.loadMoreNovelCommentReplies(query)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Comment>): String? {
        return null
    }
}
