package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.novel.MarkedNovel
import com.mrl.pixiv.common.data.novel.NovelMarkersResp
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.util.filterBlockedTags
import com.mrl.pixiv.common.repository.util.filterNormalNovel
import com.mrl.pixiv.common.repository.util.queryParams
import kotlinx.coroutines.CancellationException

class NovelMarkerPagingSource(
    private val loadInitial: suspend () -> NovelMarkersResp = {
        PixivRepository.getNovelMarkers()
    },
    private val loadMore: suspend (Map<String, String>) -> NovelMarkersResp = {
        PixivRepository.loadMoreNovelMarkers(it)
    },
    private val filterVisible: (List<MarkedNovel>) -> List<MarkedNovel> = { markers ->
        val visibleNovelIds = markers
            .map { it.novel }
            .let { novels ->
                if (requireUserPreferenceValue.isR18Enabled) {
                    novels
                } else {
                    novels.filterNormalNovel()
                }
            }
            .filterBlockedTags()
            .mapTo(mutableSetOf()) { it.id }
        markers.filter { it.novel.id in visibleNovelIds }
    },
) : PagingSource<String, MarkedNovel>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, MarkedNovel> {
        return try {
            val response = if (params.key.isNullOrEmpty()) {
                loadInitial()
            } else {
                loadMore(params.key.orEmpty().queryParams)
            }
            val distinctMarkers = response.markedNovels
                .filter { (it.novelMarker?.page ?: 0) > 0 }
                .distinctBy { it.novel.id }

            LoadResult.Page(
                data = filterVisible(distinctMarkers),
                prevKey = null,
                nextKey = response.nextUrl?.takeIf { it.isNotBlank() },
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<String, MarkedNovel>): String? = null
}
