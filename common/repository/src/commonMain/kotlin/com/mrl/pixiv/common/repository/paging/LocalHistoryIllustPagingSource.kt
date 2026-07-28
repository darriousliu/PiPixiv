package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.repository.BrowsingHistoryRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.util.filterBlockedTags
import com.mrl.pixiv.common.repository.util.filterNormalIllust

class LocalHistoryIllustPagingSource(
    private val browsingHistoryRepository: BrowsingHistoryRepository,
) : PagingSource<Int, Illust>() {
    override fun getRefreshKey(state: PagingState<Int, Illust>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Illust> {
        return try {
            val offset = params.key ?: 0
            val rawData = browsingHistoryRepository.getLocalIllusts(
                limit = params.loadSize,
                offset = offset,
            )
            val data = rawData.let { illusts ->
                if (requireUserPreferenceValue.isR18Enabled) {
                    illusts
                } else {
                    illusts.filterNormalIllust()
                }
            }.filterBlockedTags()
            LoadResult.Page(
                data = data,
                prevKey = null,
                nextKey = if (rawData.size < params.loadSize) null else offset + params.loadSize,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
