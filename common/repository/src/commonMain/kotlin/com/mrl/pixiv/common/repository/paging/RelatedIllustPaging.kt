package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Filter
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.util.filterNormal
import com.mrl.pixiv.common.repository.util.queryParams

class RelatedIllustPaging(
    private val illustId: Long
) : PagingSource<String, Illust>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Illust> {
        return try {
            val resp = if (params.key.isNullOrEmpty()) {
                PixivRepository.getIllustRelated(
                    illustId = illustId,
                    filter = Filter.ANDROID.value
                )
            } else {
                PixivRepository.loadMoreIllustRelated(
                    params.key?.queryParams ?: emptyMap()
                )
            }
            val query = resp.nextUrl
            val illusts = if (requireUserPreferenceValue.isR18Enabled) {
                resp.illusts.distinctBy { it.id }
            } else {
                resp.illusts.distinctBy { it.id }.filterNormal()
            }
            LoadResult.Page(
                data = illusts,
                prevKey = params.key,
                nextKey = query
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Illust>): String? {
        return null
    }
}