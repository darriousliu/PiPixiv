package com.mrl.pixiv.search.result

import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.setting.SearchResultIllustLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchResultLayoutTest {
    @Test
    fun illustrationSearchUsesSquareLayoutByDefault() {
        val layout = resolveSearchResultContentLayout(AppViewMode.ILLUST)

        assertEquals(SearchResultContentLayout.SQUARE_ILLUST, layout)
        assertFalse(layout.compactNovelTitle)
    }

    @Test
    fun illustrationSearchCanSelectOriginalAspectRatioLayout() {
        val layout = resolveSearchResultContentLayout(
            AppViewMode.ILLUST,
            SearchResultIllustLayout.ORIGINAL_ASPECT_RATIO,
        )

        assertEquals(SearchResultContentLayout.ORIGINAL_ASPECT_RATIO_ILLUST, layout)
        assertFalse(layout.compactNovelTitle)
    }

    @Test
    fun novelSearchSelectsCompactLayoutWithoutOuterHorizontalPadding() {
        SearchResultIllustLayout.entries.forEach { illustLayout ->
            val layout = resolveSearchResultContentLayout(
                AppViewMode.NOVEL,
                illustLayout,
            )

            assertEquals(SearchResultContentLayout.COMPACT_NOVEL_LIST, layout)
            assertTrue(layout.compactNovelTitle)
            assertEquals(0, layout.novelHorizontalPadding)
        }
    }
}
