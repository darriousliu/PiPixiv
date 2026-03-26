package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.user.UserBookmarksQuery
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.util.filterBlockedTags
import com.mrl.pixiv.common.repository.util.filterNormalNovel
import com.mrl.pixiv.common.repository.util.queryParams

class CollectionNovelPagingSource(
    private val query: UserBookmarksQuery
) : PagingSource<UserBookmarksQuery, Novel>() {
    override suspend fun load(params: LoadParams<UserBookmarksQuery>): LoadResult<UserBookmarksQuery, Novel> {
        return try {
            val resp = if (params.key == null) {
                with(query) {
                    PixivRepository.getUserBookmarksNovels(restrict, userId, tag)
                }
            } else {
                PixivRepository.loadMoreUserBookmarksNovel(params.key?.toMap() ?: emptyMap())
            }
            val query = resp.nextUrl?.queryParams
            val novels = if (requireUserPreferenceValue.isR18Enabled) {
                resp.novels.distinctBy { it.id }
            } else {
                resp.novels.distinctBy { it.id }.filterNormalNovel()
            }.filterBlockedTags()
            if (query != null) {
                val nextKey = UserBookmarksQuery(
                    restrict = query["restrict"]?.let { Restrict.fromValue(it) } ?: Restrict.PUBLIC,
                    tag = query["tag"],
                    userId = query["user_id"]?.toLongOrNull() ?: this.query.userId,
                    maxBookmarkId = query["max_bookmark_id"]?.toLongOrNull()
                )
                LoadResult.Page(
                    data = novels,
                    prevKey = params.key,
                    nextKey = nextKey
                )
            } else {
                LoadResult.Page(
                    data = novels,
                    prevKey = params.key,
                    nextKey = null
                )
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<UserBookmarksQuery, Novel>): UserBookmarksQuery? {
        return null
    }
}
