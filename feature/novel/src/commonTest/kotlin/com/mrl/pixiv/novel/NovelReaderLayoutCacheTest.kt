package com.mrl.pixiv.novel

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NovelReaderLayoutCacheTest {
    private val state = NovelState(
        paragraphs = persistentListOf("first", "second"),
    )

    @Test
    fun `font size change invalidates paragraph layouts`() {
        assertNotEquals(
            state.paragraphLayoutCacheKey(),
            state.copy(fontSize = state.fontSize + 1).paragraphLayoutCacheKey(),
        )
    }

    @Test
    fun `line spacing change invalidates paragraph layouts`() {
        assertNotEquals(
            state.paragraphLayoutCacheKey(),
            state.copy(lineSpacingSp = state.lineSpacingSp + 1).paragraphLayoutCacheKey(),
        )
    }

    @Test
    fun `reader chrome changes retain paragraph layouts`() {
        assertEquals(
            state.paragraphLayoutCacheKey(),
            state.copy(showBottomSheet = true).paragraphLayoutCacheKey(),
        )
    }
}
