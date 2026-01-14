package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Filter
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.util.filterNormal
import com.mrl.pixiv.common.repository.util.queryParams

class IllustRecommendedPagingSource : PagingSource<String, Illust>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Illust> {
        return try {
            val resp = if (params.key.isNullOrEmpty()) {
                PixivRepository.getIllustRecommended(
                    filter = Filter.ANDROID.value,
                    includeRankingIllusts = true,
                    includePrivacyPolicy = true
                )
            } else {
                PixivRepository.loadMoreIllustRecommended(params.key?.queryParams ?: emptyMap())
            }
            val illusts = if (requireUserPreferenceValue.isR18Enabled) {
                (resp.illusts + resp.rankingIllusts).distinctBy { it.id }
            } else {
                (resp.illusts + resp.rankingIllusts).distinctBy { it.id }.filterNormal()
            }
            LoadResult.Page(
                data = illusts,
                prevKey = params.key,
                nextKey = resp.nextURL?.takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Illust>): String? {
        return null
    }
}