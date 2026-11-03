package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.manga.MangaSeriesDetail
import com.mrl.pixiv.common.data.manga.MangaSeriesResp
import com.mrl.pixiv.common.data.manga.MangaWatchlistEntry
import com.mrl.pixiv.common.data.manga.MangaWatchlistResp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class MangaPagingSourcesTest {
    @Test
    fun watchlistPreservesMaskedRowsAndFollowsCursorThroughEmptyPages() = runTest {
        val next = "https://app-api.pixiv.net/v1/watchlist/manga?offset=30"
        var requested: String? = null
        val source = MangaWatchlistPagingSource(
            loadInitial = { MangaWatchlistResp(listOf(null, MangaWatchlistEntry(maskText = "Restricted")), next) },
            loadMore = { requested = it; MangaWatchlistResp(emptyList(), it) },
        )
        val first = assertIs<PagingSource.LoadResult.Page<String, MangaWatchlistEntry>>(source.load(refresh()))
        assertEquals(1, first.data.size)
        assertNull(first.data.single().navigableSeriesId)
        assertEquals(next, first.nextKey)
        val second = assertIs<PagingSource.LoadResult.Page<String, MangaWatchlistEntry>>(
            source.load(PagingSource.LoadParams.Append(next, 30, false))
        )
        assertEquals(next, requested)
        assertNull(second.nextKey) // A repeated server cursor must not create an infinite append loop.
    }

    @Test
    fun seriesHeaderSurvivesEmptyFilteredPageAndOnlyInitialLoadUpdatesIt() = runTest {
        val initial = MangaSeriesDetail(12, "Series", watchlistAdded = true)
        var header: MangaSeriesDetail? = null
        var loads = 0
        val source = MangaSeriesPagingSource(
            seriesId = 12,
            onDetail = { header = it },
            loadInitial = { id ->
                assertEquals(12L, id)
                MangaSeriesResp(initial, nextUrl = "cursor")
            },
            loadMore = { loads++; MangaSeriesResp(initial.copy(watchlistAdded = false)) },
            filterWorks = { emptyList() },
        )
        val first = assertIs<PagingSource.LoadResult.Page<String, Illust>>(source.load(refresh()))
        assertEquals(initial, header)
        assertEquals("cursor", first.nextKey)
        source.load(PagingSource.LoadParams.Append("cursor", 30, false))
        assertEquals(1, loads)
        assertEquals(initial, header)
    }

    @Test
    fun cancelledLoadDoesNotTurnIntoVisibleError() = runTest {
        val source = MangaWatchlistPagingSource(loadInitial = { throw CancellationException("Disposed") })
        assertFailsWith<CancellationException> { source.load(refresh()) }
    }

    @Test
    fun failedLoadIsRetryable() = runTest {
        var attempts = 0
        val source = MangaWatchlistPagingSource(loadInitial = {
            if (attempts++ == 0) error("offline")
            MangaWatchlistResp()
        })
        assertIs<PagingSource.LoadResult.Error<String, MangaWatchlistEntry>>(source.load(refresh()))
        assertIs<PagingSource.LoadResult.Page<String, MangaWatchlistEntry>>(source.load(refresh()))
    }

    private fun refresh() = PagingSource.LoadParams.Refresh<String>(null, 30, false)
}
