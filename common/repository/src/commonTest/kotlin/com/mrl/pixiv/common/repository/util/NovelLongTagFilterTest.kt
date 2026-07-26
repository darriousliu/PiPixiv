package com.mrl.pixiv.common.repository.util

import com.mrl.pixiv.common.data.AiType
import com.mrl.pixiv.common.data.ImageUrls
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Series
import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.data.XRestrict
import com.mrl.pixiv.common.data.setting.BrowsingSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NovelLongTagFilterTest {
    @Test
    fun disabledFilterKeepsEveryNovel() {
        val novel = novelWithTags("a".repeat(31), "a/b/c/d/e/f")

        assertFalse(novel.hasDisallowedLongTag(BrowsingSettings()))
    }

    @Test
    fun lengthBoundaryIsExclusive() {
        val settings = enabledSettings()

        assertFalse(novelWithTags("a".repeat(30)).hasDisallowedLongTag(settings))
        assertTrue(novelWithTags("a".repeat(31)).hasDisallowedLongTag(settings))
    }

    @Test
    fun segmentBoundarySupportsEveryConfiguredDelimiter() {
        val settings = enabledSettings()

        assertFalse(novelWithTags("a/b#c、d/e").hasDisallowedLongTag(settings))
        assertTrue(novelWithTags("a/b#c、d/e#f").hasDisallowedLongTag(settings))
    }

    @Test
    fun anyTagCanHideTheNovel() {
        val settings = enabledSettings()
        val novel = novelWithTags("short", "a".repeat(31), "also-short")

        assertTrue(novel.hasDisallowedLongTag(settings))
    }

    @Test
    fun invalidPersistedLimitsAreClamped() {
        val settings = enabledSettings().copy(
            maxNovelTagLength = 0,
            maxNovelTagSegments = 0,
        )

        assertFalse(novelWithTags("a").hasDisallowedLongTag(settings))
        assertTrue(novelWithTags("ab").hasDisallowedLongTag(settings))
        assertTrue(novelWithTags("a/b").hasDisallowedLongTag(settings))
    }

    @Test
    fun originalResponseCanBeRefilteredAfterSettingsChange() {
        val shortTagNovel = novelWithTags("short")
        val longTagNovel = novelWithTags("a".repeat(31))
        val originalResponse = listOf(shortTagNovel, longTagNovel)

        assertEquals(
            listOf(shortTagNovel),
            originalResponse.filterNot { it.hasDisallowedLongTag(enabledSettings()) },
        )
        assertEquals(
            originalResponse,
            originalResponse.filterNot { it.hasDisallowedLongTag(BrowsingSettings()) },
        )
    }

    private fun enabledSettings() = BrowsingSettings(
        filterLongNovelTags = true,
        maxNovelTagLength = 30,
        maxNovelTagSegments = 5,
    )

    private fun novelWithTags(vararg names: String) = Novel(
        id = 1,
        title = "title",
        caption = "",
        restrict = 0,
        xRestrict = XRestrict.Normal,
        isOriginal = false,
        imageUrls = ImageUrls(),
        createDate = "",
        tags = names.map { Tag(name = it) },
        pageCount = 1,
        textLength = 1,
        user = User(),
        series = Series(),
        isBookmarked = false,
        totalBookmarks = 0,
        totalView = 0,
        visible = true,
        isMuted = false,
        isMypixivOnly = false,
        isXRestricted = false,
        novelAiType = AiType.Undefined,
    )
}
