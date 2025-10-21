package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.user.UserBookmarksIllustQuery
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.util.queryParams

class CollectionIllustPagingSource(
    private val userId: Long,
    private val query: UserBookmarksIllustQuery
) : PagingSource<UserBookmarksIllustQuery, Illust>() {
    override suspend fun load(params: LoadParams<UserBookmarksIllustQuery>): LoadResult<UserBookmarksIllustQuery, Illust> {
        return try {
            val resp = if (params.key == null) {
                with(query) {
                    PixivRepository.getUserBookmarksIllust(restrict, userId, tag, maxBookmarkId)
                }
            } else {
                PixivRepository.loadMoreUserBookmarksIllust(params.key?.toMap() ?: emptyMap())
            }
            val query = resp.nextUrl?.queryParams
            if (query != null) {
                val nextKey = UserBookmarksIllustQuery(
                    restrict = query["restrict"] ?: Restrict.PUBLIC,
                    tag = query["tag"],
                    userId = query["user_id"]?.toLongOrNull() ?: userId,
                    maxBookmarkId = query["max_bookmark_id"]?.toLongOrNull()
                )
                LoadResult.Page(
                    data = resp.illusts.distinctBy { it.id },
                    prevKey = params.key,
                    nextKey = nextKey
                )
            } else {
                LoadResult.Page(
                    data = resp.illusts.distinctBy { it.id },
                    prevKey = params.key,
                    nextKey = null
                )
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<UserBookmarksIllustQuery, Illust>): UserBookmarksIllustQuery? {
        return null
    }
}