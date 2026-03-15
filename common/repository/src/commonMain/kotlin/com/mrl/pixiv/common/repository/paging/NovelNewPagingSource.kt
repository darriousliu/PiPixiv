package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Filter
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.util.filterNormalNovel
import com.mrl.pixiv.common.repository.util.queryParams

class NovelNewPagingSource : PagingSource<String, Novel>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Novel> {
        return try {
            val resp = if (params.key.isNullOrEmpty()) {
                PixivRepository.getNovelNew(
                    filter = Filter.ANDROID,
                    offset = null
                )
            } else {
                PixivRepository.loadMoreNovelNew(params.key?.queryParams ?: emptyMap())
            }
            val novels = if (requireUserPreferenceValue.isR18Enabled) {
                resp.novels.distinctBy { it.id }
            } else {
                resp.novels.distinctBy { it.id }.filterNormalNovel()
            }
            LoadResult.Page(
                data = novels,
                prevKey = params.key,
                nextKey = resp.nextUrl?.takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Novel>): String? {
        return null
    }
}
