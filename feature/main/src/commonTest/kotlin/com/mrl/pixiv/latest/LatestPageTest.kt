package com.mrl.pixiv.latest

import com.mrl.pixiv.common.data.AppViewMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class LatestPageTest {

    @Test
    fun novelModeKeepsOriginalPagesAndAppendsNewPages() {
        assertEquals(
            listOf(
                LatestPage.Trend,
                LatestPage.Collection,
                LatestPage.Following,
            ),
            LatestPage.pagesFor(AppViewMode.ILLUST),
        )
        assertEquals(
            listOf(
                LatestPage.Trend,
                LatestPage.Collection,
                LatestPage.Following,
                LatestPage.NovelNew,
                LatestPage.NovelWatchlist,
            ),
            LatestPage.pagesFor(AppViewMode.NOVEL),
        )
    }

    @Test
    fun novelTabsKeepIndependentScrollStates() {
        val viewModel = LatestViewModel()

        assertNotSame(
            viewModel.newNovelLazyListState,
            viewModel.watchlistNovelLazyListState,
        )
    }

    @Test
    fun illustrationAndNovelModesUsePagerStatesWithMatchingPageCounts() {
        val viewModel = LatestViewModel()
        val illustPagerState = viewModel.pagerStateFor(AppViewMode.ILLUST)
        val novelPagerState = viewModel.pagerStateFor(AppViewMode.NOVEL)

        assertNotSame(illustPagerState, novelPagerState)
        assertEquals(3, illustPagerState.pageCount)
        assertEquals(5, novelPagerState.pageCount)
    }
}
