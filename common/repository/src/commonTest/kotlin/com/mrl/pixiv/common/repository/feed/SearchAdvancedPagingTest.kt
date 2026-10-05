package com.mrl.pixiv.common.repository.feed

import com.mrl.pixiv.common.data.search.IllustAdvancedFilter
import com.mrl.pixiv.common.data.search.SearchIllustQuery
import com.mrl.pixiv.common.data.search.SearchNumberRange
import com.mrl.pixiv.common.data.search.SearchSort
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SearchAdvancedPagingTest {
    @Test
    fun addingAdvancedFilterResetsManualPageAndCursor() = runTest {
        val requests = mutableListOf<FeedPageRequest>()
        val controller = PagedFeedController(this) {
            object : FeedSource<String> {
                override val capability = FeedCapability.OFFSET
                override suspend fun load(request: FeedPageRequest): FeedPage<String> {
                    requests += request
                    return FeedPage(listOf("result"), FeedKey.Offset(request.page * 30))
                }
            }
        }
        val query = SearchIllustQuery(word = "cat")
        controller.ensureLoaded(query)
        advanceUntilIdle()
        controller.nextPage()
        advanceUntilIdle()
        assertEquals(2, controller.state.value.currentPage)
        controller.ensureLoaded(query.copy(advanced = IllustAdvancedFilter(width = SearchNumberRange(1000))))
        advanceUntilIdle()
        assertEquals(1, controller.state.value.currentPage)
        assertFalse(controller.state.value.hasPreviousPage)
        assertEquals(FeedPageRequest(), requests.last())
    }

    @Test
    fun freeAdvancedSearchUsesSupportedPaginatedSort() {
        assertEquals(SearchSort.DATE_DESC, effectiveSearchSort(SearchSort.POPULAR_DESC, false, true))
        assertEquals(SearchSort.POPULAR_DESC, effectiveSearchSort(SearchSort.POPULAR_DESC, true, true))
        assertEquals(SearchSort.POPULAR_DESC, effectiveSearchSort(SearchSort.POPULAR_DESC, false, false))
        assertEquals(SearchSort.DATE_ASC, effectiveSearchSort(SearchSort.DATE_ASC, false, true))
        assertEquals(FeedCapability.OFFSET, SearchIllustFeedSource(SearchIllustQuery(word = "cat",
            advanced = IllustAdvancedFilter(width = SearchNumberRange(1000))), isPremium = false, isIdSearch = false).capability)
    }
}
