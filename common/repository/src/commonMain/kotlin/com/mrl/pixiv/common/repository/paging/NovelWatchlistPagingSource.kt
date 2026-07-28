package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.novel.NovelWatchlistResp
import com.mrl.pixiv.common.data.novel.NovelWatchlistSeries
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.util.queryParams

class NovelWatchlistPagingSource(
    private val loadInitial: suspend () -> NovelWatchlistResp = {
        PixivRepository.getNovelWatchlist()
    },
    private val loadMore: suspend (Map<String, String>) -> NovelWatchlistResp = {
        PixivRepository.loadMoreNovelWatchlist(it)
    },
) : PagingSource<String, NovelWatchlistSeries>() {

    override suspend fun load(
        params: LoadParams<String>,
    ): LoadResult<String, NovelWatchlistSeries> = try {
        val response = if (params.key.isNullOrBlank()) {
            loadInitial()
        } else {
            loadMore(params.key.orEmpty().queryParams)
        }
        LoadResult.Page(
            data = response.series.orEmpty().filterNotNull(),
            prevKey = null,
            nextKey = response.nextUrl?.takeIf(String::isNotBlank),
        )
    } catch (error: Exception) {
        LoadResult.Error(error)
    }

    override fun getRefreshKey(
        state: PagingState<String, NovelWatchlistSeries>,
    ): String? = null
}
