package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.discovery.CommunityUserPreview
import com.mrl.pixiv.common.data.discovery.CommunityUsersKind
import com.mrl.pixiv.common.data.discovery.CommunityUsersResponse
import com.mrl.pixiv.common.repository.CommunityRepository
import com.mrl.pixiv.common.repository.communityCursorQuery
import kotlinx.coroutines.CancellationException

class CommunityUsersPagingSource(
    private val kind: CommunityUsersKind,
    private val seedUserId: Long? = null,
    private val loadInitial: suspend () -> CommunityUsersResponse = {
        CommunityRepository.getUsers(kind, seedUserId)
    },
    private val loadMore: suspend (Map<String, String>) -> CommunityUsersResponse = {
        CommunityRepository.getUsersNext(kind, it)
    },
) : PagingSource<String, CommunityUserPreview>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, CommunityUserPreview> = try {
        val response = if (params.key == null) loadInitial() else loadMore(
            communityCursorQuery(params.key!!, when (kind) {
                CommunityUsersKind.RECOMMENDED -> "v1/user/recommended"
                CommunityUsersKind.FOLLOWERS -> "v1/user/follower"
                CommunityUsersKind.RELATED -> error("Related users do not support pagination")
            })
        )
        LoadResult.Page(
            data = response.userPreviews.distinctBy { it.user.id },
            prevKey = null,
            nextKey = if (kind == CommunityUsersKind.RELATED) null else
                response.nextUrl?.takeIf { it.isNotBlank() && it != params.key },
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        LoadResult.Error(error)
    }

    override fun getRefreshKey(state: PagingState<String, CommunityUserPreview>): String? = null
}
