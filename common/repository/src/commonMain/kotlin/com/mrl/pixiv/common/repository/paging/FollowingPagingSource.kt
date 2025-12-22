package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Filter
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.follow.FollowingReq
import com.mrl.pixiv.common.data.user.UserPreview
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.util.queryParams

class FollowingPagingSource(
    private val userId: Long,
    private val restrict: Restrict
) : PagingSource<FollowingReq, UserPreview>() {
    override suspend fun load(params: LoadParams<FollowingReq>): LoadResult<FollowingReq, UserPreview> {

        return try {
            val resp = if (params.key == null) {
                PixivRepository.getUserFollowing(userId = userId, restrict = restrict)
            } else {
                PixivRepository.loadMoreUserFollowing(params.key?.toMap() ?: emptyMap())
            }
            val query = resp.nextUrl?.queryParams
            if (query != null) {
                val nextKey = FollowingReq(
                    filter = query["filter"] ?: Filter.ANDROID.value,
                    restrict = query["restrict"]?.let { Restrict.fromValue(it) } ?: Restrict.PUBLIC,
                    userId = query["user_id"]?.toLongOrNull() ?: userId,
                    offset = query["offset"]?.toIntOrNull()
                )
                LoadResult.Page(
                    data = resp.userPreviews.distinctBy { it.user.id },
                    prevKey = params.key,
                    nextKey = nextKey
                )
            } else {
                LoadResult.Page(
                    data = resp.userPreviews.distinctBy { it.user.id },
                    prevKey = params.key,
                    nextKey = null
                )
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<FollowingReq, UserPreview>): FollowingReq? {
        return null
    }
}