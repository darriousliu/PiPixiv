package com.mrl.pixiv.search.result

import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.search.SearchNumberRange
import com.mrl.pixiv.common.data.search.SearchSort
import com.mrl.pixiv.common.data.search.SearchTarget
import com.mrl.pixiv.common.data.setting.SearchFilterPreferences
import com.mrl.pixiv.common.data.setting.SearchSettings
import com.mrl.pixiv.search.SearchState.SearchFilter
import kotlinx.datetime.LocalDateRange

internal fun resolveInitialSearchState(
    searchWords: String,
    searchSettings: SearchSettings,
    searchMode: AppViewMode,
    isIdSearch: Boolean = false,
): SearchResultState {
    if (isIdSearch) return SearchResultState(searchWords = searchWords)

    val remembered = searchSettings.lastFilter(searchMode)
    val startDate = remembered?.startDate
    val endDate = remembered?.endDate
    return SearchResultState(
        searchWords = searchWords,
        searchFilter = resolveInitialSearchFilter(searchSettings, searchMode),
        bookmarkNumRange = remembered?.bookmarkNumRange?.let {
            (it.min ?: 0)..(it.max ?: Int.MAX_VALUE)
        },
        bookmarkStringRange = remembered?.bookmarkStringRange,
        searchDateRange = if (startDate != null && endDate != null) {
            LocalDateRange(startDate, endDate)
        } else null,
    )
}

internal fun resolveInitialSearchFilter(
    searchSettings: SearchSettings,
    searchMode: AppViewMode,
): SearchFilter {
    val preferences = searchSettings.lastFilter(searchMode) ?: SearchFilterPreferences(
        sort = searchSettings.defaultSearchSort,
        searchTarget = searchSettings.defaultSearchTarget,
        searchAiType = searchSettings.defaultSearchAiType,
        contentFilter = searchSettings.defaultContentFilter,
    )
    val searchTarget = when (searchMode) {
        AppViewMode.ILLUST -> when (preferences.searchTarget) {
            SearchTarget.TEXT,
            SearchTarget.KEYWORD -> SearchTarget.TITLE_AND_CAPTION

            else -> preferences.searchTarget
        }

        AppViewMode.NOVEL -> when (preferences.searchTarget) {
            SearchTarget.TITLE_AND_CAPTION,
            SearchTarget.TEXT -> SearchTarget.KEYWORD

            else -> preferences.searchTarget
        }
    }
    val searchSort = when {
        searchMode == AppViewMode.NOVEL &&
                preferences.sort in novelUnsupportedSearchSorts ->
            SearchSort.POPULAR_DESC

        else -> preferences.sort
    }
    return SearchFilter(
        sort = searchSort,
        searchTarget = searchTarget,
        searchAiType = preferences.searchAiType,
        contentFilter = preferences.contentFilter,
        illustAdvanced = preferences.illustAdvanced,
        novelAdvanced = preferences.novelAdvanced,
    )
}

internal fun SearchResultState.toFilterPreferences(): SearchFilterPreferences =
    SearchFilterPreferences(
        sort = searchFilter.sort,
        searchTarget = searchFilter.searchTarget,
        searchAiType = searchFilter.searchAiType,
        contentFilter = searchFilter.contentFilter,
        illustAdvanced = searchFilter.illustAdvanced,
        novelAdvanced = searchFilter.novelAdvanced,
        bookmarkNumRange = bookmarkNumRange?.let {
            SearchNumberRange(it.first, it.last.takeIf { max -> max != Int.MAX_VALUE })
        },
        bookmarkStringRange = bookmarkStringRange,
        startDate = searchDateRange?.start,
        endDate = searchDateRange?.endInclusive,
    )

private val novelUnsupportedSearchSorts = setOf(
    SearchSort.POPULAR_MALE_DESC,
    SearchSort.POPULAR_FEMALE_DESC,
)
