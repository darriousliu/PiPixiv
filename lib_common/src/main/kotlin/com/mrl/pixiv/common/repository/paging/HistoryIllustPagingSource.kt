package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.util.queryParams
import org.koin.core.annotation.Factory

@Factory
class HistoryIllustPagingSource : PagingSource<String, Illust>() {
    override fun getRefreshKey(state: PagingState<String, Illust>): String? {
        return null
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Illust> {
        return try {
            val resp = if (params.key.isNullOrEmpty()) {
                PixivRepository.getUserBrowsingHistoryIllusts()
            } else {
                PixivRepository.loadMoreUserBrowsingHistoryIllusts(
                    params.key?.queryParams ?: emptyMap()
                )
            }
            val query = resp.nextUrl
            LoadResult.Page(
                data = resp.illusts.distinctBy { it.id },
                prevKey = params.key,
                nextKey = query
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}