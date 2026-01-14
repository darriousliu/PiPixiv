package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.user.UserPreview
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.util.queryParams

class SearchUserPagingSource(
    private val word: String,
    private val isIdSearch: Boolean
) : PagingSource<String, UserPreview>() {
    override fun getRefreshKey(state: PagingState<String, UserPreview>): String? {
        return null
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, UserPreview> {
        return try {
            if (isIdSearch) {
                val userId = word.toLongOrNull() ?: return LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )
                val resp = PixivRepository.getUserDetail(userId = userId)
                return LoadResult.Page(
                    data = listOf(UserPreview(resp.user, emptyList(), emptyList(), false)),
                    prevKey = null,
                    nextKey = null
                )
            }
            val resp = if (params.key == null) {
                PixivRepository.searchUser(word = word)
            } else {
                PixivRepository.searchUserNext(params.key!!.queryParams)
            }
            LoadResult.Page(
                data = resp.userPreviews,
                prevKey = params.key,
                nextKey = resp.nextUrl
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
