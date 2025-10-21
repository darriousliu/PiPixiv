package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.util.queryParams

class IllustFollowingPagingSource(
    private val restrict: Restrict
) : PagingSource<Long, Illust>() {
    override fun getRefreshKey(state: PagingState<Long, Illust>): Long? {
        return null
    }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Illust> {
        return try {
            val resp = if (params.key == null) {
                PixivRepository.getFollowingIllusts(
                    restrict = restrict,
                )
            } else {
                PixivRepository.getFollowingIllusts(
                    restrict = restrict,
                    offset = params.key,
                )
            }
            LoadResult.Page(
                data = resp.illusts,
                prevKey = null,
                nextKey = resp.nextUrl?.queryParams["offset"]?.toLongOrNull()
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}