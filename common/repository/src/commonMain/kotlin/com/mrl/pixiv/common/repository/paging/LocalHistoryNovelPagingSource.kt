package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.repository.BrowsingHistoryRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.util.filterBlockedTags
import com.mrl.pixiv.common.repository.util.filterNormalNovel

class LocalHistoryNovelPagingSource(
    private val browsingHistoryRepository: BrowsingHistoryRepository,
) : PagingSource<Int, Novel>() {
    init {
        invalidateOnNovelFilterSettingsChanges()
    }

    override fun getRefreshKey(state: PagingState<Int, Novel>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Novel> {
        return try {
            val offset = params.key ?: 0
            val rawData = browsingHistoryRepository.getLocalNovels(
                limit = params.loadSize,
                offset = offset,
            )
            val data = rawData.let { novels ->
                if (requireUserPreferenceValue.isR18Enabled) {
                    novels
                } else {
                    novels.filterNormalNovel()
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
