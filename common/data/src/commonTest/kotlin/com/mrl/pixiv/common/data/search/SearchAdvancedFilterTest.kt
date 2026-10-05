package com.mrl.pixiv.common.data.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SearchAdvancedFilterTest {
    @Test
    fun numericBoundsRejectInvalidInputAndKeepOpenRanges() {
        listOf("-1" to "", "5" to "4", "abc" to "", "2147483648" to "").forEach { (min, max) ->
            assertNull(parseSearchNumberRange(min, max))
        }
        assertEquals(SearchNumberRange(), parseSearchNumberRange("", ""))
        assertEquals(SearchNumberRange(0, null), parseSearchNumberRange("0", ""))
        assertEquals(SearchNumberRange(null, 100), parseSearchNumberRange("", "100"))
    }

    @Test
    fun advancedIllustQueryPreservesExistingConstraintsAndOmitsUnsetBounds() {
        val query = SearchIllustQuery(word = "空 白", offset = 60, startDate = "2026-01-01",
            advanced = IllustAdvancedFilter(width = SearchNumberRange(800),
                height = SearchNumberRange(max = 2000), ratio = SearchRatio.PORTRAIT, tool = "CLIP STUDIO PAINT"))
        val parameters = query.toMap()
        assertEquals("空 白", parameters["word"])
        assertEquals("60", parameters["offset"])
        assertEquals("2026-01-01", parameters["start_date"])
        assertEquals("800", parameters["width_min"])
        assertNull(parameters["width_max"])
        assertEquals("2000", parameters["height_max"])
        assertEquals("portrait", parameters["ratio_pattern"])
        assertEquals("CLIP STUDIO PAINT", parameters["tool"])
    }

    @Test
    fun novelWireParametersUseMinutesAndApiNames() {
        val parameters = SearchNovelQuery(word = "猫", advanced = NovelAdvancedFilter(
            textLength = SearchNumberRange(100, 1000), wordCount = SearchNumberRange(max = 800),
            readingTime = SearchNumberRange(5, 30), language = "en", genre = 7,
            originalOnly = true, replaceableOnly = true,
        )).toMap()
        assertEquals("100", parameters["text_length_min"])
        assertEquals("800", parameters["word_count_max"])
        assertEquals("5", parameters["reading_time_min"])
        assertEquals("30", parameters["reading_time_max"])
        assertEquals("en", parameters["lang"])
        assertEquals("7", parameters["genre"])
        assertEquals("true", parameters["is_original_only"])
        assertEquals("true", parameters["is_replaceable_only"])
        assertFalse(NovelAdvancedFilter().isActive)
        assertFalse(NovelAdvancedFilter().toMap().containsKey("is_original_only"))
    }

    @Test
    fun dynamicSelectionMustRemainInCurrentOptions() {
        val options = SearchOptionsResponse(
            illust = SearchIllustOptions(tool = SearchTools(listOf("Photoshop"))),
            novel = SearchNovelOptions(lang = SearchLanguages(listOf(SearchLanguageOption("en", "English"))),
                genre = SearchGenres(listOf(SearchGenreOption(3, "Fantasy")))),
        )
        assertNull(IllustAdvancedFilter(tool = "Invented").validatedBy(options).tool)
        assertEquals("Photoshop", IllustAdvancedFilter(tool = "Photoshop").validatedBy(options).tool)
        val novel = NovelAdvancedFilter(language = "en", genre = 99).validatedBy(options)
        assertEquals("en", novel.language)
        assertNull(novel.genre)
    }
}
