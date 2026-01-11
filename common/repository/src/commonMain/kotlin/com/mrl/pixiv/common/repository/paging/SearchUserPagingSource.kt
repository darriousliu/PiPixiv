package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.user.UserPreview
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.util.queryParams

class SearchUserPagingSource(
    private val word: String,
) : PagingSource<String, UserPreview>() {
    override fun getRefreshKey(state: PagingState<String, UserPreview>): String? {
        return null
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, UserPreview> {
        return try {
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
