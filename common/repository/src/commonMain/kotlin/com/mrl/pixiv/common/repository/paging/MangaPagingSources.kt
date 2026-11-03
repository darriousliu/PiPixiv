package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.manga.MangaSeriesDetail
import com.mrl.pixiv.common.data.manga.MangaSeriesResp
import com.mrl.pixiv.common.data.manga.MangaWatchlistEntry
import com.mrl.pixiv.common.data.manga.MangaWatchlistResp
import com.mrl.pixiv.common.data.manga.UserMangaSeriesResp
import com.mrl.pixiv.common.repository.ReadingRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.util.filterBlockedTags
import com.mrl.pixiv.common.repository.util.filterNormalIllust
import kotlinx.coroutines.CancellationException

/** Apply the same local age/tag filters as the existing illustration feeds. */
fun List<Illust>.visibleMangaWorks(): List<Illust> =
    (if (requireUserPreferenceValue.isR18Enabled) this else filterNormalIllust())
        .filterBlockedTags()
        .filter { it.visible != false && it.isMuted != true }

class MangaSeriesPagingSource(
    private val seriesId: Long,
    private val onDetail: (MangaSeriesDetail) -> Unit = {},
    private val loadInitial: suspend (Long) -> MangaSeriesResp = ReadingRepository::mangaSeries,
    private val loadMore: suspend (String) -> MangaSeriesResp = ReadingRepository::mangaSeriesNext,
    private val filterWorks: (List<Illust>) -> List<Illust> = { it.visibleMangaWorks() },
) : PagingSource<String, Illust>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Illust> = try {
        val response = params.key?.let { loadMore(it) } ?: loadInitial(seriesId)
        if (params.key == null) onDetail(response.detail)
        LoadResult.Page(
            data = filterWorks(response.illusts.distinctBy { it.id }),
            prevKey = null,
            nextKey = response.nextUrl.nextPageAfter(params.key),
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        LoadResult.Error(error)
    }

    override fun getRefreshKey(state: PagingState<String, Illust>): String? = null
}

class MangaWatchlistPagingSource(
    private val loadInitial: suspend () -> MangaWatchlistResp = ReadingRepository::mangaWatchlist,
    private val loadMore: suspend (String) -> MangaWatchlistResp = ReadingRepository::mangaWatchlistNext,
) : PagingSource<String, MangaWatchlistEntry>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, MangaWatchlistEntry> = try {
        val response = params.key?.let { loadMore(it) } ?: loadInitial()
        LoadResult.Page(
            // Masked entries are intentional server results, including entries without IDs.
            data = response.series.orEmpty().filterNotNull(),
            prevKey = null,
            nextKey = response.nextUrl.nextPageAfter(params.key),
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        LoadResult.Error(error)
    }

    override fun getRefreshKey(state: PagingState<String, MangaWatchlistEntry>): String? = null
}

class UserMangaSeriesPagingSource(
    private val userId: Long,
    private val loadInitial: suspend (Long) -> UserMangaSeriesResp = ReadingRepository::userMangaSeries,
    private val loadMore: suspend (String) -> UserMangaSeriesResp = ReadingRepository::userMangaSeriesNext,
) : PagingSource<String, MangaSeriesDetail>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, MangaSeriesDetail> = try {
        val response = params.key?.let { loadMore(it) } ?: loadInitial(userId)
        LoadResult.Page(
            data = response.series.distinctBy { it.id },
            prevKey = null,
            nextKey = response.nextUrl.nextPageAfter(params.key),
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        LoadResult.Error(error)
    }

    override fun getRefreshKey(state: PagingState<String, MangaSeriesDetail>): String? = null
}

private fun String?.nextPageAfter(current: String?): String? =
    this?.takeIf { it.isNotBlank() && it != current }
