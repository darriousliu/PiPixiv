package com.mrl.pixiv.common.repository.feed

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PagedFeedControllerTest {
    @Test
    fun derivesStrideFromServerOffsetAndUsesItForPageJump() = runTest {
        val requests = mutableListOf<FeedPageRequest>()
        val controller = PagedFeedController(this) {
            TestFeedSource { request ->
                requests += request
                val offset = request.offset()
                FeedPage(
                    items = listOf("page-${request.page}"),
                    nextKey = FeedKey.Offset(offset + 30),
                )
            }
        }

        controller.ensureLoaded(queryKey = "cats")
        advanceUntilIdle()

        assertTrue(controller.state.value.canJumpToPage)
        controller.loadPage(3)
        advanceUntilIdle()

        assertEquals(3, controller.state.value.currentPage)
        assertEquals(FeedKey.Offset(60), requests.last().key)

        val requestCount = requests.size
        controller.loadPage(Int.MAX_VALUE)
        advanceUntilIdle()
        assertEquals(requestCount, requests.size)
    }

    @Test
    fun usesExactAdjacentKeysAndDisablesJumpWhenStrideChanges() = runTest {
        val requests = mutableListOf<FeedPageRequest>()
        val controller = PagedFeedController(this) {
            TestFeedSource { request ->
                requests += request
                val nextOffset = when (request.page) {
                    1 -> 30
                    2 -> 70
                    else -> null
                }
                FeedPage(
                    items = listOf("page-${request.page}"),
                    nextKey = nextOffset?.let(FeedKey::Offset),
                )
            }
        }

        controller.ensureLoaded(queryKey = "cats")
        advanceUntilIdle()
        controller.nextPage()
        advanceUntilIdle()

        assertEquals(2, controller.state.value.currentPage)
        assertFalse(controller.state.value.canJumpToPage)
        assertTrue(controller.state.value.hasPreviousPage)
        assertTrue(controller.state.value.hasNextPage)

        controller.nextPage()
        advanceUntilIdle()
        assertEquals(FeedKey.Offset(70), requests.last().key)

        controller.previousPage()
        advanceUntilIdle()
        assertEquals(FeedKey.Offset(30), requests.last().key)
        assertEquals(2, controller.state.value.currentPage)
    }

    @Test
    fun keepsLoadedPageWhileRequestIsPendingAndAfterFailure() = runTest {
        val secondPageGate = CompletableDeferred<Unit>()
        val failure = IllegalStateException("page failed")
        val controller = PagedFeedController(this) {
            TestFeedSource { request ->
                if (request.page == 2) {
                    secondPageGate.await()
                    throw failure
                }
                FeedPage(
                    items = listOf("page-1"),
                    nextKey = FeedKey.Offset(30),
                )
            }
        }

        controller.ensureLoaded(queryKey = "cats")
        advanceUntilIdle()
        controller.nextPage()
        runCurrent()

        assertEquals(1, controller.state.value.currentPage)
        assertEquals(2, controller.state.value.requestedPage)
        assertEquals(listOf("page-1"), controller.state.value.items)

        secondPageGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, controller.state.value.currentPage)
        assertNull(controller.state.value.requestedPage)
        assertEquals(listOf("page-1"), controller.state.value.items)
        assertEquals(failure, controller.state.value.error)
    }

    @Test
    fun cancelledLateResponseCannotOverwriteNewerPage() = runTest {
        val secondPageGate = CompletableDeferred<Unit>()
        val thirdPageGate = CompletableDeferred<Unit>()
        val controller = PagedFeedController(this) {
            TestFeedSource { request ->
                when (request.page) {
                    1 -> FeedPage(
                        items = listOf("page-1"),
                        nextKey = FeedKey.Offset(30),
                    )

                    2 -> withContext(NonCancellable) {
                        secondPageGate.await()
                        FeedPage(
                            items = listOf("stale-page-2"),
                            nextKey = FeedKey.Offset(60),
                        )
                    }

                    else -> {
                        thirdPageGate.await()
                        FeedPage(
                            items = listOf("page-3"),
                            nextKey = FeedKey.Offset(90),
                        )
                    }
                }
            }
        }

        controller.ensureLoaded(queryKey = "cats")
        advanceUntilIdle()
        controller.nextPage()
        runCurrent()
        controller.loadPage(3)
        runCurrent()

        thirdPageGate.complete(Unit)
        runCurrent()
        assertEquals(3, controller.state.value.currentPage)
        assertEquals(listOf("page-3"), controller.state.value.items)

        secondPageGate.complete(Unit)
        advanceUntilIdle()
        assertEquals(3, controller.state.value.currentPage)
        assertEquals(listOf("page-3"), controller.state.value.items)
        assertNull(controller.state.value.error)
    }

    @Test
    fun resetsOnlyWhenQueryIdentityChanges() = runTest {
        var activeQuery = "cats"
        var loadCount = 0
        val controller = PagedFeedController(this) {
            val sourceQuery = activeQuery
            TestFeedSource { request ->
                loadCount++
                FeedPage(
                    items = listOf("$sourceQuery-${request.page}"),
                    nextKey = FeedKey.Offset(request.offset() + 30),
                )
            }
        }

        controller.ensureLoaded(queryKey = activeQuery)
        advanceUntilIdle()
        controller.loadPage(2)
        advanceUntilIdle()
        assertEquals(2, controller.state.value.currentPage)

        controller.ensureLoaded(queryKey = activeQuery)
        advanceUntilIdle()
        assertEquals(2, controller.state.value.currentPage)
        assertEquals(2, loadCount)

        activeQuery = "dogs"
        controller.ensureLoaded(queryKey = activeQuery)
        advanceUntilIdle()

        assertEquals(1, controller.state.value.currentPage)
        assertEquals(listOf("dogs-1"), controller.state.value.items)
        assertEquals(3, loadCount)
        assertTrue(controller.state.value.scrollToTopEventId >= 3)
    }

    @Test
    fun singlePageSourceNeverExposesNavigation() = runTest {
        val controller = PagedFeedController(this) {
            TestFeedSource(capability = FeedCapability.SINGLE_PAGE) {
                FeedPage(
                    items = listOf("preview"),
                )
            }
        }

        controller.ensureLoaded(queryKey = "popular-preview")
        advanceUntilIdle()

        val state = controller.state.value
        assertFalse(state.hasPreviousPage)
        assertFalse(state.hasNextPage)
        assertFalse(state.canJumpToPage)
        assertEquals(FeedCapability.SINGLE_PAGE, state.capability)
    }

    @Test
    fun refreshClearsStaleFuturePageKeys() = runTest {
        var firstPageNextOffset = 30
        val requests = mutableListOf<FeedPageRequest>()
        val controller = PagedFeedController(this) {
            TestFeedSource { request ->
                requests += request
                FeedPage(
                    items = listOf("page-${request.page}"),
                    nextKey = FeedKey.Offset(
                        if (request.page == 1) firstPageNextOffset else request.offset() + 30
                    ),
                )
            }
        }

        controller.ensureLoaded(queryKey = "cats")
        advanceUntilIdle()
        controller.loadPage(3)
        advanceUntilIdle()

        firstPageNextOffset = 40
        controller.loadPage(1)
        advanceUntilIdle()
        assertFalse(controller.state.value.canJumpToPage)

        val requestCount = requests.size
        controller.loadPage(3)
        advanceUntilIdle()
        assertEquals(requestCount, requests.size)
    }

    @Test
    fun refreshReloadsCurrentPageWithLatestSourceState() = runTest {
        var sourceVersion = 1
        val requests = mutableListOf<FeedPageRequest>()
        val controller = PagedFeedController(this) {
            val version = sourceVersion
            TestFeedSource { request ->
                requests += request
                FeedPage(
                    items = listOf("version-$version-page-${request.page}"),
                    nextKey = FeedKey.Offset(request.offset() + 30),
                )
            }
        }

        controller.ensureLoaded(queryKey = "novels")
        advanceUntilIdle()
        controller.nextPage()
        advanceUntilIdle()
        val scrollEventBeforeRefresh = controller.state.value.scrollToTopEventId

        sourceVersion = 2
        controller.refresh()
        advanceUntilIdle()

        assertEquals(2, controller.state.value.currentPage)
        assertEquals(listOf("version-2-page-2"), controller.state.value.items)
        assertEquals(FeedKey.Offset(30), requests.last().key)
        assertEquals(scrollEventBeforeRefresh, controller.state.value.scrollToTopEventId)
    }

    @Test
    fun pendingOldQueryCannotOverwriteNewQuery() = runTest {
        val oldQueryGate = CompletableDeferred<Unit>()
        var activeQuery = "cats"
        val controller = PagedFeedController(this) {
            val sourceQuery = activeQuery
            TestFeedSource {
                if (sourceQuery == "cats") {
                    withContext(NonCancellable) {
                        oldQueryGate.await()
                        FeedPage(items = listOf("stale-cats"))
                    }
                } else {
                    FeedPage(items = listOf("dogs"))
                }
            }
        }

        controller.ensureLoaded(queryKey = activeQuery)
        runCurrent()
        activeQuery = "dogs"
        controller.ensureLoaded(queryKey = activeQuery)
        runCurrent()

        assertEquals(listOf("dogs"), controller.state.value.items)
        oldQueryGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("dogs"), controller.state.value.items)
        assertNull(controller.state.value.error)
    }

    @Test
    fun parsesOnlyValidNonNegativeServerOffsets() {
        assertEquals(
            30,
            "https://app-api.pixiv.net/v1/search/illust?word=cats&offset=30".nextOffset(),
        )
        assertNull("https://app-api.pixiv.net/v1/search/illust?offset=invalid".nextOffset())
        assertNull("https://app-api.pixiv.net/v1/search/illust?offset=-30".nextOffset())
        assertNull("not a url".nextOffset())
    }

    @Test
    fun rejectsMissingOrNegativeOffsetsAfterFirstPage() {
        assertFailsWith<IllegalArgumentException> {
            FeedPageRequest(page = 2).offsetValue()
        }
        assertFailsWith<IllegalArgumentException> {
            FeedPageRequest(key = FeedKey.Offset(-1), page = 2).offsetValue()
        }
    }
}

private class TestFeedSource(
    override val capability: FeedCapability = FeedCapability.OFFSET,
    private val loader: suspend (FeedPageRequest) -> FeedPage<String>,
) : FeedSource<String> {
    override suspend fun load(request: FeedPageRequest): FeedPage<String> = loader(request)
}

private fun FeedPageRequest.offset(): Int {
    return (key as? FeedKey.Offset)?.value ?: 0
}
