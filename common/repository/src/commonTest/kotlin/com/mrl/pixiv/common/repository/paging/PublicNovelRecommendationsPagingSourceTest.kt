package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import com.mrl.pixiv.common.data.novel.PublicNovelPreview
import com.mrl.pixiv.common.data.novel.PublicNovelRecommendations
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PublicNovelRecommendationsPagingSourceTest {
    @Test
    fun orderedIdsDriveFinitePaginationEvenWhenOneBatchIsUnavailable() = runTest {
        val requested = mutableListOf<List<String>>()
        val ids = (2..33).map(Int::toString)
        val source = PublicNovelRecommendationsPagingSource(
            novelId = 100,
            initial = {
                PublicNovelRecommendations(
                    listOf(PublicNovelPreview("1")),
                    listOf("1", "invalid", "0", "2") + ids,
                )
            },
            more = {
                requested += it
                if (requested.size == 1) PublicNovelRecommendations()
                else PublicNovelRecommendations(it.map { id -> PublicNovelPreview(id) })
            },
            filter = { it },
        )
        val first = assertIs<PagingSource.LoadResult.Page<List<String>, PublicNovelPreview>>(
            source.load(PagingSource.LoadParams.Refresh(null, 30, false))
        )
        assertEquals(ids, first.nextKey)
        val second = assertIs<PagingSource.LoadResult.Page<List<String>, PublicNovelPreview>>(
            source.load(PagingSource.LoadParams.Append(first.nextKey!!, 30, false))
        )
        assertEquals(ids.take(30), requested[0])
        assertEquals(emptyList(), second.data)
        assertEquals(listOf("32", "33"), second.nextKey)
        val third = assertIs<PagingSource.LoadResult.Page<List<String>, PublicNovelPreview>>(
            source.load(PagingSource.LoadParams.Append(second.nextKey!!, 30, false))
        )
        assertEquals(listOf("32", "33"), third.data.map { it.id })
        assertNull(third.nextKey)
    }

    @Test
    fun initialFailureIsExposedAndCanBeRetried() = runTest {
        var attempts = 0
        val source = PublicNovelRecommendationsPagingSource(
            novelId = 1,
            initial = { if (attempts++ == 0) error("Unavailable") else PublicNovelRecommendations() },
            filter = { it },
        )
        val params = PagingSource.LoadParams.Refresh<List<String>>(null, 30, false)
        assertIs<PagingSource.LoadResult.Error<List<String>, PublicNovelPreview>>(source.load(params))
        assertIs<PagingSource.LoadResult.Page<List<String>, PublicNovelPreview>>(source.load(params))
    }
}
