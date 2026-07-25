package com.mrl.pixiv.common.repository.feed

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
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

    fun reset(page: Int = 1) {
        pageKeys.clear()
        state.value = PagedFeedState(currentPage = page)
        loadPage(page)
    }

    fun refresh() {
        loadPage(state.value.currentPage)
    }

    fun nextPage() {
        val current = state.value
        if (current.hasNextPage) {
            loadPage(current.currentPage + 1)
        }
    }

    fun previousPage() {
        val currentPage = state.value.currentPage
        if (currentPage > 1) {
            loadPage(currentPage - 1)
        }
    }

    fun loadPage(page: Int) {
        if (page < 1) return

        val source = sourceProvider()
        val key = when (source.capability) {
            FeedCapability.OFFSET -> FeedKey.Offset((page - 1) * source.pageSize)
            FeedCapability.CURSOR -> if (page == 1) null else pageKeys[page] ?: return
            FeedCapability.SINGLE_PAGE -> if (page == 1) null else return
        }

        loadJob?.cancel()
        loadJob = scope.launch(Dispatchers.IO) {
            state.update {
                it.copy(
                    currentPage = page,
                    isLoading = true,
                    error = null,
                    capability = source.capability,
                    canJumpToPage = source.capability == FeedCapability.OFFSET,
                )
            }

            runCatching {
                source.load(
                    FeedPageRequest(
                        key = key,
                        page = page,
                        pageSize = source.pageSize,
                    )
                )
            }.onSuccess { result ->
                result.nextKey?.let { pageKeys[page + 1] = it }
                result.prevKey?.let { pageKeys[page - 1] = it }
                state.value = PagedFeedState(
                    items = result.items,
                    currentPage = result.page,
                    isLoading = false,
                    error = null,
                    hasNextPage = result.hasNextPage,
                    canJumpToPage = result.canJumpToPage,
                    capability = result.capability,
                )
            }.onFailure { throwable ->
                state.update {
                    it.copy(
                        currentPage = page,
                        isLoading = false,
                        error = throwable,
                        capability = source.capability,
                        canJumpToPage = source.capability == FeedCapability.OFFSET,
                    )
                }
            }
        }
    }
}
