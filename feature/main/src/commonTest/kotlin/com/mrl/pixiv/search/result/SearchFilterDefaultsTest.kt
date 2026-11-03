package com.mrl.pixiv.search.result

import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.ImageUrls
import com.mrl.pixiv.common.data.MetaSinglePage
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.data.XRestrict
import com.mrl.pixiv.common.data.search.IllustAdvancedFilter
import com.mrl.pixiv.common.data.search.NovelAdvancedFilter
import com.mrl.pixiv.common.data.search.SearchAiType
import com.mrl.pixiv.common.data.search.SearchArtworkType
import com.mrl.pixiv.common.data.search.SearchContentFilter
import com.mrl.pixiv.common.data.search.SearchNovelQuery
import com.mrl.pixiv.common.data.search.SearchNumberRange
import com.mrl.pixiv.common.data.search.SearchRatio
import com.mrl.pixiv.common.data.search.SearchSort
import com.mrl.pixiv.common.data.search.SearchTarget
import com.mrl.pixiv.common.data.setting.SearchSettings
import com.mrl.pixiv.search.SearchState.SearchFilter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchFilterDefaultsTest {
    @Test
    fun oldPreferencesStillUseConfiguredDefaultsAndPrivacySetting() {
        val defaults = Json.decodeFromString<SearchSettings>(
            """{"defaultContentFilter":{"artworkType":"GIF_ONLY","seriesOnly":true}}"""
        )
        val initial = resolveInitialSearchFilter(defaults, AppViewMode.ILLUST)
        assertEquals(null, initial.contentFilter.showR18)
        assertEquals(SearchArtworkType.GIF_ONLY, initial.contentFilter.artworkType)
        assertEquals(true, initial.contentFilter.seriesOnly)
        assertEquals(null, defaults.lastIllustFilter)
        assertEquals(null, defaults.lastNovelFilter)
    }

    @Test
    fun changingOnlySortKeepsFollowingTheGlobalPrivacySettingAfterRestart() {
        val initial = resolveInitialSearchState("tag", SearchSettings(), AppViewMode.ILLUST)
        val changed =
            initial.copy(searchFilter = initial.searchFilter.copy(sort = SearchSort.DATE_DESC))
        val settings =
            SearchSettings().rememberFilter(AppViewMode.ILLUST, changed.toFilterPreferences())
        val persisted = Json.decodeFromString<SearchSettings>(Json.encodeToString(settings))
        val filter = resolveInitialSearchFilter(persisted, AppViewMode.ILLUST).contentFilter
        val restrictedIllust = Illust(
            id = 1, title = "", type = Type.Illust, imageUrls = ImageUrls(), user = User(),
            width = 100, height = 100, metaSinglePage = MetaSinglePage(), totalView = 0,
            totalBookmarks = 0, isBookmarked = false, xRestrict = XRestrict(1),
        )

        assertEquals(null, filter.showR18)
        assertTrue(filter.matches(restrictedIllust, defaultShowR18 = true))
        assertFalse(filter.matches(restrictedIllust, defaultShowR18 = false))
    }

    @Test
    fun idSearchDoesNotRestoreKeywordFiltersOrBookmarkTags() {
        val filtered = SearchResultState(
            bookmarkStringRange = "1000users入り",
            searchDateRange = LocalDateRange(LocalDate(2026, 1, 1), LocalDate(2026, 10, 1)),
            searchFilter = SearchFilter(
                sort = SearchSort.DATE_ASC,
                novelAdvanced = NovelAdvancedFilter(originalOnly = true),
            ),
        )
        val settings =
            SearchSettings().rememberFilter(AppViewMode.NOVEL, filtered.toFilterPreferences())

        assertEquals(
            SearchResultState(searchWords = "12345678"),
            resolveInitialSearchState("12345678", settings, AppViewMode.NOVEL, isIdSearch = true),
        )
        assertEquals(
            filtered.copy(searchWords = "next tag"),
            resolveInitialSearchState("next tag", settings, AppViewMode.NOVEL),
        )
    }

    @Test
    fun illustrationSearchUsesEveryConfiguredDefault() {
        val settings = SearchSettings(
            defaultSearchTarget = SearchTarget.TITLE_AND_CAPTION,
            defaultSearchSort = SearchSort.POPULAR_FEMALE_DESC,
            defaultSearchAiType = SearchAiType.SHOW_AI,
        )

        val filter = resolveInitialSearchFilter(settings, AppViewMode.ILLUST)

        assertEquals(SearchTarget.TITLE_AND_CAPTION, filter.searchTarget)
        assertEquals(SearchSort.POPULAR_FEMALE_DESC, filter.sort)
        assertEquals(SearchAiType.SHOW_AI, filter.searchAiType)
    }

    @Test
    fun novelSearchNormalizesIllustrationOnlyDefaults() {
        val settings = SearchSettings(
            defaultSearchTarget = SearchTarget.TITLE_AND_CAPTION,
            defaultSearchSort = SearchSort.POPULAR_MALE_DESC,
            defaultSearchAiType = SearchAiType.SHOW_AI,
        )

        val filter = resolveInitialSearchFilter(settings, AppViewMode.NOVEL)

        assertEquals(SearchTarget.KEYWORD, filter.searchTarget)
        assertEquals(SearchSort.POPULAR_DESC, filter.sort)
        assertEquals(SearchAiType.SHOW_AI, filter.searchAiType)
    }

    @Test
    fun appliedFiltersSurviveSerializationIndependentlyForEachMode() {
        val illustState = SearchResultState(
            searchWords = "illustration tag",
            searchFilter = SearchFilter(
                searchTarget = SearchTarget.EXACT_MATCH_FOR_TAGS,
                sort = SearchSort.POPULAR_FEMALE_DESC,
                contentFilter = SearchContentFilter(
                    artworkType = SearchArtworkType.GIF_ONLY,
                    showR18 = false,
                ),
                illustAdvanced = IllustAdvancedFilter(
                    width = SearchNumberRange(min = 1024),
                    ratio = SearchRatio.LANDSCAPE,
                    tool = "Photoshop",
                ),
            ),
            bookmarkNumRange = 1000..Int.MAX_VALUE,
            searchDateRange = LocalDateRange(LocalDate(2026, 1, 1), LocalDate(2026, 10, 1)),
        )
        val novelState = SearchResultState(
            searchWords = "novel tag",
            searchFilter = SearchFilter(
                searchTarget = SearchTarget.KEYWORD,
                sort = SearchSort.DATE_DESC,
                searchAiType = SearchAiType.SHOW_AI,
                contentFilter = SearchContentFilter(seriesOnly = true, showR18 = true),
                novelAdvanced = NovelAdvancedFilter(
                    textLength = SearchNumberRange(3000, 10000),
                    language = "ja",
                    genre = 1,
                    originalOnly = true,
                ),
            ),
            bookmarkStringRange = "1000users入り",
        )
        val settings = SearchSettings()
            .rememberFilter(AppViewMode.ILLUST, illustState.toFilterPreferences())
            .rememberFilter(AppViewMode.NOVEL, novelState.toFilterPreferences())
        val restored = Json.decodeFromString<SearchSettings>(Json.encodeToString(settings))

        assertEquals(
            illustState.copy(searchWords = "new illustration tag"),
            resolveInitialSearchState("new illustration tag", restored, AppViewMode.ILLUST, false),
        )
        assertEquals(
            novelState.copy(searchWords = "new novel tag"),
            resolveInitialSearchState("new novel tag", restored, AppViewMode.NOVEL, false),
        )

        val cleared = SearchResultState(
            searchFilter = SearchFilter(contentFilter = SearchContentFilter(showR18 = false)),
        )
        val afterClearing =
            restored.rememberFilter(AppViewMode.NOVEL, cleared.toFilterPreferences())
        assertEquals(
            cleared,
            resolveInitialSearchState("", afterClearing, AppViewMode.NOVEL, false),
        )
        assertEquals(restored.lastIllustFilter, afterClearing.lastIllustFilter)
    }

    @Test
    fun selectingGlobalDefaultAgainUpdatesBothModesWithoutLosingOtherConditions() {
        val illust = SearchResultState(
            searchFilter = SearchFilter(
                sort = SearchSort.DATE_ASC,
                illustAdvanced = IllustAdvancedFilter(ratio = SearchRatio.PORTRAIT),
            ),
            bookmarkNumRange = 100..500,
        ).toFilterPreferences()
        val novel = SearchResultState(
            searchFilter = SearchFilter(
                sort = SearchSort.DATE_DESC,
                novelAdvanced = NovelAdvancedFilter(originalOnly = true),
            ),
        ).toFilterPreferences()
        val settings = SearchSettings(lastIllustFilter = illust, lastNovelFilter = novel)
        val updated = settings.updateFilterDefaults { copy(sort = settings.defaultSearchSort) }

        assertEquals(SearchSort.POPULAR_DESC, updated.defaultSearchSort)
        assertEquals(illust.copy(sort = SearchSort.POPULAR_DESC), updated.lastIllustFilter)
        assertEquals(novel.copy(sort = SearchSort.POPULAR_DESC), updated.lastNovelFilter)
        val normalizedNovel = updated.updateFilterDefaults {
            copy(searchTarget = SearchTarget.TITLE_AND_CAPTION, sort = SearchSort.POPULAR_MALE_DESC)
        }
        assertEquals(
            SearchTarget.KEYWORD,
            resolveInitialSearchFilter(normalizedNovel, AppViewMode.NOVEL).searchTarget,
        )
        assertEquals(
            SearchSort.POPULAR_DESC,
            resolveInitialSearchFilter(normalizedNovel, AppViewMode.NOVEL).sort,
        )
    }

    @Test
    fun novelQuerySendsConfiguredAiFilter() {
        val query = SearchNovelQuery(
            word = "test",
            searchAiType = SearchAiType.SHOW_AI,
        )

        assertEquals(
            SearchAiType.SHOW_AI.value.toString(),
            query.toMap()["search_ai_type"],
        )
    }
}
