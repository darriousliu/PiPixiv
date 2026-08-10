package com.mrl.pixiv.novel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NovelMetadataBottomSheetTest {
    @Test
    fun validSeriesIdAcceptsOnlyPositiveValues() {
        assertEquals(42L, validNovelSeriesId(42L))
        assertNull(validNovelSeriesId(0L))
        assertNull(validNovelSeriesId(-1L))
        assertNull(validNovelSeriesId(null))
    }

    @Test
    fun bookmarkCountIncreasesAfterLocallyBookmarkingNovel() {
        assertEquals(
            11L,
            adjustedNovelBookmarkCount(
                totalBookmarks = 10L,
                initiallyBookmarked = false,
                currentlyBookmarked = true,
            ),
        )
    }

    @Test
    fun bookmarkCountDecreasesAfterLocallyRemovingBookmark() {
        assertEquals(
            9L,
            adjustedNovelBookmarkCount(
                totalBookmarks = 10L,
                initiallyBookmarked = true,
                currentlyBookmarked = false,
            ),
        )
    }

    @Test
    fun bookmarkCountIsUnchangedWhenBookmarkStateMatchesResponse() {
        assertEquals(
            10L,
            adjustedNovelBookmarkCount(
                totalBookmarks = 10L,
                initiallyBookmarked = true,
                currentlyBookmarked = true,
            ),
        )
        assertEquals(
            10L,
            adjustedNovelBookmarkCount(
                totalBookmarks = 10L,
                initiallyBookmarked = false,
                currentlyBookmarked = false,
            ),
        )
    }

    @Test
    fun bookmarkCountNeverBecomesNegative() {
        assertEquals(
            0L,
            adjustedNovelBookmarkCount(
                totalBookmarks = 0L,
                initiallyBookmarked = true,
                currentlyBookmarked = false,
            ),
        )
    }
}
