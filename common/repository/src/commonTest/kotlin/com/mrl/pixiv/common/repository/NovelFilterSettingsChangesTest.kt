package com.mrl.pixiv.common.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.setting.BrowsingSettings
import com.mrl.pixiv.common.data.setting.PreviewImageQuality
import com.mrl.pixiv.common.repository.paging.invalidateOnNovelFilterSettingsChanges
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
    fun activeNovelPagingSourceIsInvalidatedAfterRelevantChange() {
        val source = TestPagingSource().apply {
            invalidateOnNovelFilterSettingsChanges()
        }
        val initial = BrowsingSettings()

        NovelFilterSettingsChanges.notifyIfChanged(
            initial,
            initial.copy(autoHidePreviewControls = false),
        )
        assertFalse(source.invalid)

        NovelFilterSettingsChanges.notifyIfChanged(
            initial,
            initial.copy(maxNovelTagLength = initial.maxNovelTagLength + 1),
        )
        assertTrue(source.invalid)
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
