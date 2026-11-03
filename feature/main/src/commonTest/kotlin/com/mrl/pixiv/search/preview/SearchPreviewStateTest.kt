package com.mrl.pixiv.search.preview

import com.mrl.pixiv.common.data.AiType
import com.mrl.pixiv.common.data.ImageUrls
import com.mrl.pixiv.common.data.MetaSinglePage
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.data.search.TrendTagIllust
import com.mrl.pixiv.common.data.search.TrendingTag
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SearchPreviewStateTest {
    @Test
    fun failedRefreshKeepsTagsAndStopsLoading() = runTest {
        val previous = SearchPreviewState(refreshing = true, trendingTags = persistentListOf(tag))

        val result = resolveTrendingTags(previous) { error("Network unavailable") }

        assertSame(previous.trendingTags, result.trendingTags)
        assertFalse(result.refreshing)
        assertTrue(result.failed)
    }

    @Test
    fun successfulRetryReplacesTagsAndClearsTheError() = runTest {
        val result = resolveTrendingTags(SearchPreviewState(refreshing = true, failed = true)) {
            listOf(tag)
        }

        assertEquals(listOf(tag), result.trendingTags)
        assertFalse(result.refreshing)
        assertFalse(result.failed)
    }

    @Test
    fun emptySuccessfulResponseIsAnEmptyStateNotAnError() = runTest {
        val result = resolveTrendingTags(SearchPreviewState(refreshing = true)) { emptyList() }

        assertTrue(result.trendingTags.isEmpty())
        assertFalse(result.refreshing)
        assertFalse(result.failed)
    }

    @Test
    fun cancellingAnObsoleteModeRequestDoesNotProduceAFailureState() = runTest {
        val cancellation = CancellationException("Mode changed")

        val thrown = assertFailsWith<CancellationException> {
            resolveTrendingTags(SearchPreviewState(refreshing = true)) { throw cancellation }
        }

        assertSame(cancellation, thrown)
    }

    private val tag = TrendingTag(
        tag = "风景",
        illust = TrendTagIllust(
            id = 1,
            title = "湖边",
            type = "illust",
            imageUrls = ImageUrls(),
            caption = "",
            restrict = 0,
            user = User(id = 1, name = "作者"),
            tags = emptyList(),
            tools = emptyList(),
            createDate = "2026-11-03",
            pageCount = 1,
            width = 800,
            height = 600,
            sanityLevel = 2,
            xRestrict = 0,
            metaSinglePage = MetaSinglePage(),
            totalView = 0,
            totalBookmarks = 0,
            isBookmarked = false,
            visible = true,
            isMuted = false,
            illustAiType = AiType.NotAiGeneratedWork,
            illustBookStyle = 0,
        ),
    )
}
