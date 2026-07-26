package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Filter
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.novel.NovelSeriesDetail
import com.mrl.pixiv.common.data.novel.NovelSeriesResp
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.util.filterBlockedTags
import com.mrl.pixiv.common.repository.util.filterNormalNovel
import com.mrl.pixiv.common.repository.util.queryParams

class NovelSeriesPagingSource(
    private val seriesId: Long,
    private val onSeriesDetail: (NovelSeriesDetail) -> Unit = {},
    private val loadInitial: suspend (Long) -> NovelSeriesResp = {
        PixivRepository.getNovelSeries(
            seriesId = it,
            filter = Filter.ANDROID,
            offset = null,
        )
    },
    private val loadMore: suspend (Map<String, String>) -> NovelSeriesResp = {
        PixivRepository.loadMoreNovelSeries(it)
    },
    private val isR18Enabled: () -> Boolean = {
        requireUserPreferenceValue.isR18Enabled
    },
) : PagingSource<String, Novel>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Novel> = try {
        val response = if (params.key.isNullOrBlank()) {
            loadInitial(seriesId).also { onSeriesDetail(it.novelSeriesDetail) }
        } else {
            loadMore(params.key.orEmpty().queryParams)
        }
        val distinctNovels = response.novels.distinctBy(Novel::id)
        val visibleNovels = if (isR18Enabled()) {
            distinctNovels
        } else {
            distinctNovels.filterNormalNovel()
        }.filterBlockedTags()

        LoadResult.Page(
            data = visibleNovels,
            prevKey = null,
            nextKey = response.nextUrl?.takeIf(String::isNotBlank),
        )
    } catch (error: Exception) {
        LoadResult.Error(error)
    }

    override fun getRefreshKey(state: PagingState<String, Novel>): String? = null
}
