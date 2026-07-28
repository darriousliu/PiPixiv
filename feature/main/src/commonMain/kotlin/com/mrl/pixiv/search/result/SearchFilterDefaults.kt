package com.mrl.pixiv.search.result

import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.search.SearchSort
import com.mrl.pixiv.common.data.search.SearchTarget
import com.mrl.pixiv.common.data.setting.SearchSettings
import com.mrl.pixiv.search.SearchState.SearchFilter

internal fun resolveInitialSearchFilter(
    searchSettings: SearchSettings,
    searchMode: AppViewMode,
): SearchFilter {
    val searchTarget = when (searchMode) {
        AppViewMode.ILLUST -> when (searchSettings.defaultSearchTarget) {
            SearchTarget.TEXT,
            SearchTarget.KEYWORD -> SearchTarget.TITLE_AND_CAPTION

            else -> searchSettings.defaultSearchTarget
        }

        AppViewMode.NOVEL -> when (searchSettings.defaultSearchTarget) {
            SearchTarget.TITLE_AND_CAPTION,
            SearchTarget.TEXT -> SearchTarget.KEYWORD

            else -> searchSettings.defaultSearchTarget
        }
    }
    val searchSort = when {
        searchMode == AppViewMode.NOVEL &&
                searchSettings.defaultSearchSort in novelUnsupportedSearchSorts ->
            SearchSort.POPULAR_DESC

        else -> searchSettings.defaultSearchSort
    }
    return SearchFilter(
        sort = searchSort,
        searchTarget = searchTarget,
        searchAiType = searchSettings.defaultSearchAiType,
    )
}

private val novelUnsupportedSearchSorts = setOf(
    SearchSort.POPULAR_MALE_DESC,
    SearchSort.POPULAR_FEMALE_DESC,
)
