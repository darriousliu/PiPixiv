package com.mrl.pixiv.common.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.setting.BrowsingSettings
import com.mrl.pixiv.common.data.setting.PreviewImageQuality
import com.mrl.pixiv.common.repository.paging.novelFilterPagingFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NovelFilterSettingsChangesTest {
    @Test
    fun onlyNovelFilterFieldsPublishChanges() {
        val initial = BrowsingSettings()
        var notifications = 0
        val unregister = NovelFilterSettingsChanges.register {
            notifications += 1
        }

        try {
            NovelFilterSettingsChanges.notifyIfChanged(initial, initial)
            NovelFilterSettingsChanges.notifyIfChanged(
                initial,
                initial.copy(previewImageQuality = PreviewImageQuality.HIGH),
            )
            assertEquals(0, notifications)

            NovelFilterSettingsChanges.notifyIfChanged(
                initial,
                initial.copy(filterLongNovelTags = true),
            )
            NovelFilterSettingsChanges.notifyIfChanged(
                initial,
                initial.copy(maxNovelTagLength = initial.maxNovelTagLength + 1),
            )
            NovelFilterSettingsChanges.notifyIfChanged(
                initial,
                initial.copy(maxNovelTagSegments = initial.maxNovelTagSegments + 1),
            )
            assertEquals(3, notifications)
        } finally {
            unregister()
        }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun settingsRefreshOnlyTheActivePagingGenerationUntilItsCacheScopeIsCancelled() = runTest {
        val pagingJob = Job(backgroundScope.coroutineContext.job)
        val pagingScope = CoroutineScope(backgroundScope.coroutineContext + pagingJob)
        val sources = mutableListOf<TestPagingSource>()
        val pages = novelFilterPagingFlow(PagingConfig(pageSize = 20)) {
            TestPagingSource().also { sources.add(it) }
        }.cachedIn(pagingScope)
        backgroundScope.launch { pages.collect() }
        runCurrent()
        val firstSource = sources.single()
        val initial = BrowsingSettings()

        NovelFilterSettingsChanges.notifyIfChanged(
            initial,
            initial.copy(previewImageQuality = PreviewImageQuality.HIGH),
        )
        assertFalse(firstSource.invalid)

        val updated = initial.copy(maxNovelTagLength = initial.maxNovelTagLength + 1)
        NovelFilterSettingsChanges.notifyIfChanged(initial, updated)
        runCurrent()
        assertTrue(firstSource.invalid)
        assertEquals(2, sources.size)
        val activeSource = sources.last()

        pagingJob.cancelAndJoin()
        NovelFilterSettingsChanges.notifyIfChanged(updated, initial)
        assertFalse(activeSource.invalid)
    }
}

private class TestPagingSource : PagingSource<Int, Int>() {
    override fun getRefreshKey(state: PagingState<Int, Int>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> =
        LoadResult.Page(
            data = emptyList(),
            prevKey = null,
            nextKey = null,
        )
}
