package com.mrl.pixiv.novel

import kotlin.test.Test
import kotlin.test.assertEquals

class NovelChapterNavigationTest {
    @Test
    fun initialLoadKeepsTheEntryChapterState() {
        assertEquals(
            100L,
            novelChapterStateKey(
                entryNovelId = 100L,
                loadedNovelId = null,
            ),
        )
        assertEquals(
            100L,
            novelChapterStateKey(
                entryNovelId = 100L,
                loadedNovelId = 100L,
            ),
        )
    }

    @Test
    fun loadedChapterUsesItsOwnListStateKey() {
        assertEquals(
            101L,
            novelChapterStateKey(
                entryNovelId = 100L,
                loadedNovelId = 101L,
            ),
        )
    }

    @Test
    fun forwardAndBackwardNavigationAlwaysChangeTheListStateKey() {
        val currentKey = novelChapterStateKey(
            entryNovelId = 100L,
            loadedNovelId = 100L,
        )
        val nextKey = novelChapterStateKey(
            entryNovelId = 100L,
            loadedNovelId = 101L,
        )
        val previousKey = novelChapterStateKey(
            entryNovelId = 100L,
            loadedNovelId = 99L,
        )
        val returnKey = novelChapterStateKey(
            entryNovelId = 100L,
            loadedNovelId = 100L,
        )

        assertEquals(100L, currentKey)
        assertEquals(101L, nextKey)
        assertEquals(99L, previousKey)
        assertEquals(100L, returnKey)
    }
}
