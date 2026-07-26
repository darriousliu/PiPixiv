package com.mrl.pixiv.common.repository.feed

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PagedFeedController<T : Any>(
    private val scope: CoroutineScope,
    private val sourceProvider: () -> FeedSource<T>,
) {
    private val pageKeys = mutableMapOf<Int, FeedKey>()
    val state: StateFlow<PagedFeedState<T>>
        field = MutableStateFlow(PagedFeedState<T>())

    private var loadJob: Job? = null
    private var latestRequestId = 0L
    private var queryKey: Any? = null
    private var hasQueryKey = false
    private var offsetStride: Int? = null
    private var offsetJumpDisabled = false
    private var scrollToTopEventId = 0L

    fun ensureLoaded(queryKey: Any?) {
        if (hasQueryKey && this.queryKey == queryKey) return
        this.queryKey = queryKey
        hasQueryKey = true
        reset()
    }

    private fun reset() {
        latestRequestId++
        loadJob?.cancel()
        loadJob = null
        pageKeys.clear()
        offsetStride = null
        offsetJumpDisabled = false
        state.value = PagedFeedState(scrollToTopEventId = scrollToTopEventId)
        loadPage(page = 1, scrollToTopOnSuccess = true)
    }

    fun refresh() {
        loadPage(
            page = state.value.currentPage,
            scrollToTopOnSuccess = false,
        )
    }

    fun nextPage() {
        val current = state.value
        val nextPage = current.currentPage.nextPageNumber()
        if (current.hasNextPage && nextPage != null) {
            loadPage(
                page = nextPage,
                scrollToTopOnSuccess = true,
            )
        }
    }

    fun previousPage() {
        val current = state.value
        if (current.hasPreviousPage) {
            loadPage(
                page = current.currentPage - 1,
                scrollToTopOnSuccess = true,
            )
        }
    }

    fun loadPage(page: Int) {
        loadPage(page = page, scrollToTopOnSuccess = true)
    }

    private fun loadPage(
        page: Int,
        scrollToTopOnSuccess: Boolean,
    ) {
        if (page < 1) return

        val source = sourceProvider()
        if (!canResolvePage(page, source.capability)) return
        val key = resolvePageKey(page, source.capability)

        val requestId = ++latestRequestId
        loadJob?.cancel()
        state.update {
            it.copy(
                requestedPage = page,
                error = null,
                capability = source.capability,
            )
        }
        loadJob = scope.launch {
            try {
                val result = source.load(
                    FeedPageRequest(
                        key = key,
                        page = page,
                    )
                )
                currentCoroutineContext().ensureActive()
                if (requestId != latestRequestId) return@launch

                recordPageKeys(
                    page = page,
                    requestKey = key,
                    nextKey = result.nextKey,
                    capability = source.capability,
                )
                if (scrollToTopOnSuccess) {
                    scrollToTopEventId++
                }
                val previousPage = page.takeIf { it > 1 }?.minus(1)
                val nextPage = page.nextPageNumber()
                state.value = PagedFeedState(
                    items = result.items,
                    currentPage = page,
                    requestedPage = null,
                    error = null,
                    hasPreviousPage = previousPage?.let {
                        canResolvePage(page = it, capability = source.capability)
                    } == true,
                    hasNextPage = result.hasNextPage && nextPage?.let {
                        canResolvePage(page = it, capability = source.capability)
                    } == true,
                    canJumpToPage = canJumpToPage(source.capability),
                    capability = source.capability,
                    scrollToTopEventId = scrollToTopEventId,
                )
            } catch (throwable: CancellationException) {
                throw throwable
            } catch (throwable: Throwable) {
                if (requestId != latestRequestId) return@launch
                state.update {
                    it.copy(
                        requestedPage = null,
                        error = throwable,
                    )
                }
            }
        }
    }

    private fun recordPageKeys(
        page: Int,
        requestKey: FeedKey?,
        nextKey: FeedKey?,
        capability: FeedCapability,
    ) {
        if (page > 1 && requestKey != null) {
            pageKeys[page] = requestKey
        }

        updateOffsetStride(
            page = page,
            requestKey = requestKey,
            nextKey = nextKey,
            capability = capability,
        )

        pageKeys.keys.removeAll { it > page }
        validNextKey(
            page = page,
            requestKey = requestKey,
            nextKey = nextKey,
            capability = capability,
        )?.let { validNextKey ->
            page.nextPageNumber()?.let { pageKeys[it] = validNextKey }
        }
    }

    private fun updateOffsetStride(
        page: Int,
        requestKey: FeedKey?,
        nextKey: FeedKey?,
        capability: FeedCapability,
    ) {
        if (capability != FeedCapability.OFFSET || offsetJumpDisabled) return
        if (nextKey == null) {
            if (page == 1) disableOffsetJump()
            return
        }
        val currentOffset = when {
            page == 1 -> 0
            requestKey is FeedKey.Offset -> requestKey.value
            else -> {
                disableOffsetJump()
                return
            }
        }
        val nextOffset = (nextKey as? FeedKey.Offset)?.value
        if (nextOffset == null) {
            disableOffsetJump()
            return
        }
        val candidateValue = nextOffset.toLong() - currentOffset.toLong()
        val candidate = candidateValue.takeIf { it in 1..Int.MAX_VALUE.toLong() }?.toInt()
        if (
            candidate == null ||
            safeOffset(page = page, stride = candidate) != currentOffset ||
            (offsetStride != null && offsetStride != candidate)
        ) {
            disableOffsetJump()
            return
        }
        offsetStride = candidate
    }

    private fun disableOffsetJump() {
        offsetStride = null
        offsetJumpDisabled = true
    }

    private fun validNextKey(
        page: Int,
        requestKey: FeedKey?,
        nextKey: FeedKey?,
        capability: FeedCapability,
    ): FeedKey? {
        if (capability != FeedCapability.OFFSET) return null
        val currentOffset = when {
            page == 1 -> 0
            requestKey is FeedKey.Offset -> requestKey.value
            else -> return null
        }
        return (nextKey as? FeedKey.Offset)?.takeIf { it.value > currentOffset }
    }

    private fun canResolvePage(page: Int, capability: FeedCapability): Boolean {
        if (page < 1) return false
        if (page == 1) return true
        if (pageKeys[page]?.isValidFor(capability) == true) return true
        return capability == FeedCapability.OFFSET && offsetKeyForPage(page) != null
    }

    private fun resolvePageKey(page: Int, capability: FeedCapability): FeedKey? {
        if (page == 1) return null
        return pageKeys[page]?.takeIf { it.isValidFor(capability) }
            ?: if (capability == FeedCapability.OFFSET) offsetKeyForPage(page) else null
    }

    private fun offsetKeyForPage(page: Int): FeedKey.Offset? {
        val stride = offsetStride ?: return null
        return safeOffset(page = page, stride = stride)?.let(FeedKey::Offset)
    }

    private fun canJumpToPage(capability: FeedCapability): Boolean {
        return capability == FeedCapability.OFFSET &&
            !offsetJumpDisabled &&
            offsetStride != null
    }

    private fun safeOffset(page: Int, stride: Int): Int? {
        if (page < 1 || stride <= 0) return null
        val pageIndex = page - 1
        if (pageIndex > Int.MAX_VALUE / stride) return null
        return pageIndex * stride
    }

    private fun FeedKey.isValidFor(capability: FeedCapability): Boolean {
        return when (capability) {
            FeedCapability.OFFSET -> this is FeedKey.Offset && value >= 0
            FeedCapability.SINGLE_PAGE -> false
        }
    }

    private fun Int.nextPageNumber(): Int? {
        return takeIf { it < Int.MAX_VALUE }?.plus(1)
    }
}
