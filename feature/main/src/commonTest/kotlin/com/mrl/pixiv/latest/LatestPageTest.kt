package com.mrl.pixiv.latest

import com.mrl.pixiv.common.data.AppViewMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class LatestPageTest {

    @Test
    fun illustrationAndNovelModesHaveIndependentThreePageSets() {
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
                LatestPage.NovelRecommended,
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
            viewModel.recommendedNovelLazyListState,
            viewModel.newNovelLazyListState,
        )
        assertNotSame(
            viewModel.newNovelLazyListState,
            viewModel.watchlistNovelLazyListState,
        )
        assertNotSame(
            viewModel.recommendedNovelLazyListState,
            viewModel.watchlistNovelLazyListState,
        )
    }
}
