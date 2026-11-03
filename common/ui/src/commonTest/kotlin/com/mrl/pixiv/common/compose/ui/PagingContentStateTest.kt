package com.mrl.pixiv.common.compose.ui

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PagingContentStateTest {
    @Test
    fun initialLoadOwnsThePageEvenWhenAppendIsAlsoLoading() {
        val presentation = pagingPresentation(states(LoadState.Loading, LoadState.Loading), 0)
        assertEquals(PagingStatus.Loading, presentation.page)
        assertEquals(PagingStatus.None, presentation.append)
        assertFalse(presentation.refreshError)
    }

    @Test
    fun refreshWithCachedItemsDoesNotShowPageOrAppendLoading() {
        val presentation = pagingPresentation(states(LoadState.Loading, LoadState.Loading), 12)
        assertEquals(PagingStatus.None, presentation.page)
        assertEquals(PagingStatus.None, presentation.append)
    }

    @Test
    fun refreshingHidesThePreviousAppendFailure() {
        val presentation = pagingPresentation(states(LoadState.Loading, failure), 12)
        assertEquals(PagingStatus.None, presentation.append)
        assertFalse(presentation.refreshError)
    }

    @Test
    fun failedRefreshRetainsItemsAndOffersOneRetry() {
        val presentation = pagingPresentation(states(failure, failure), 12)
        assertEquals(PagingStatus.None, presentation.page)
        assertTrue(presentation.refreshError)
        assertEquals(PagingStatus.None, presentation.append)
    }

    @Test
    fun appendUsesFooterOnlyAfterRefreshCompletes() {
        assertEquals(
            PagingStatus.Loading,
            pagingPresentation(states(idle, LoadState.Loading), 12).append
        )
        assertEquals(PagingStatus.Error, pagingPresentation(states(idle, failure), 12).append)
        assertEquals(PagingStatus.None, pagingPresentation(states(idle, idle), 12).append)
    }

    @Test
    fun zeroItemsAreEmptyOnlyAfterLoadingCompletesSuccessfully() {
        assertEquals(PagingStatus.Empty, pagingPresentation(states(idle, idle), 0).page)
        assertEquals(PagingStatus.Error, pagingPresentation(states(failure, idle), 0).page)
        assertEquals(PagingStatus.Error, pagingPresentation(states(idle, failure), 0).page)
        assertEquals(
            PagingStatus.Loading,
            pagingPresentation(states(idle, LoadState.Loading), 0).page
        )
    }

    private fun states(refresh: LoadState, append: LoadState): CombinedLoadStates {
        val source = LoadStates(refresh = refresh, prepend = idle, append = append)
        return CombinedLoadStates(refresh, idle, append, source)
    }

    private companion object {
        val idle = LoadState.NotLoading(endOfPaginationReached = true)
        val failure = LoadState.Error(IllegalStateException("offline"))
    }
}
