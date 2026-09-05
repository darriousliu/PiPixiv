package com.mrl.pixiv.history

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HistorySearchTest {
    @Test
    fun changingAndClearingSearchReusesLoadedPages() = runTest {
        val items = listOf("apple", "apricot", "banana")
        var loadCount = 0
        val search = MutableStateFlow("")
        val flow = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
            TestPagingSource(items) { loadCount++ }
        }.flow.filterHistoryBySearch(search, backgroundScope) { item, query ->
            item.contains(query, ignoreCase = true)
        }
        val presenter = object : PagingDataPresenter<String>(StandardTestDispatcher(testScheduler)) {
            override suspend fun presentPagingDataEvent(event: PagingDataEvent<String>) = Unit
        }
        backgroundScope.launch { flow.collectLatest(presenter::collectFrom) }

        runCurrent()
        assertEquals(items, presenter.snapshot().items)

        for ((query, expected) in listOf(
            "ap" to listOf("apple", "apricot"),
            "app" to listOf("apple"),
            "APPLE" to listOf("apple"),
            "missing" to emptyList(),
            "" to items,
        )) {
            search.value = query
            runCurrent()
            assertEquals(expected, presenter.snapshot().items, "query=$query")
        }
        assertEquals(1, loadCount, "Search should filter cached pages without reloading the source")
    }

    @Test
    fun returningToFilteredHistoryReplaysPagesAndAllowsAnotherSearch() = runTest {
        val items = listOf("apple", "apricot", "banana")
        var loadCount = 0
        val search = MutableStateFlow("")
        val flow = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
            TestPagingSource(items) { loadCount++ }
        }.flow.filterHistoryBySearch(search, backgroundScope) { item, query ->
            item.contains(query)
        }
        fun presenter() = object : PagingDataPresenter<String>(StandardTestDispatcher(testScheduler)) {
            override suspend fun presentPagingDataEvent(event: PagingDataEvent<String>) = Unit
        }

        val firstPresenter = presenter()
        val firstCollection = backgroundScope.launch { flow.collectLatest(firstPresenter::collectFrom) }
        runCurrent()
        search.value = "app"
        runCurrent()
        assertEquals(listOf("apple"), firstPresenter.snapshot().items)
        firstCollection.cancel()
        runCurrent()

        val returningPresenter = presenter()
        backgroundScope.launch { flow.collectLatest(returningPresenter::collectFrom) }
        runCurrent()
        assertEquals(listOf("apple"), returningPresenter.snapshot().items)

        search.value = "banana"
        runCurrent()
        assertEquals(listOf("banana"), returningPresenter.snapshot().items)
        assertEquals(1, loadCount)
    }

    private class TestPagingSource(
        private val items: List<String>,
        private val onLoad: () -> Unit,
    ) : PagingSource<Int, String>() {
        override fun getRefreshKey(state: PagingState<Int, String>): Int? = null

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
            onLoad()
            return LoadResult.Page(data = items, prevKey = null, nextKey = null)
        }
    }
}
