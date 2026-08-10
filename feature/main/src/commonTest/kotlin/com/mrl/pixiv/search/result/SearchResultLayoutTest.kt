package com.mrl.pixiv.search.result

import com.mrl.pixiv.common.data.AppViewMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchResultLayoutTest {
    @Test
    fun illustrationSearchSelectsOriginalAspectRatioLayout() {
        val layout = resolveSearchResultContentLayout(AppViewMode.ILLUST)

        assertEquals(SearchResultContentLayout.ORIGINAL_ASPECT_RATIO_ILLUST, layout)
        assertFalse(layout.compactNovelTitle)
    }

    @Test
    fun novelSearchSelectsCompactLayoutWithoutOuterHorizontalPadding() {
        val layout = resolveSearchResultContentLayout(AppViewMode.NOVEL)

        assertEquals(SearchResultContentLayout.COMPACT_NOVEL_LIST, layout)
        assertTrue(layout.compactNovelTitle)
        assertEquals(0, layout.novelHorizontalPadding)
    }
}
