package com.mrl.pixiv.common.data.setting

import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.search.IllustAdvancedFilter
import com.mrl.pixiv.common.data.search.NovelAdvancedFilter
import com.mrl.pixiv.common.data.search.SearchAiType
import com.mrl.pixiv.common.data.search.SearchContentFilter
import com.mrl.pixiv.common.data.search.SearchNumberRange
import com.mrl.pixiv.common.data.search.SearchSort
import com.mrl.pixiv.common.data.search.SearchTarget
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class SearchSettings(
    val defaultSearchTarget: SearchTarget = SearchTarget.PARTIAL_MATCH_FOR_TAGS,
    val defaultSearchSort: SearchSort = SearchSort.POPULAR_DESC,
    val defaultSearchAiType: SearchAiType = SearchAiType.HIDE_AI,
    val defaultContentFilter: SearchContentFilter = SearchContentFilter(),
    val searchResultDisplayMode: SearchResultDisplayMode = SearchResultDisplayMode.INFINITE_SCROLL,
    val lastIllustFilter: SearchFilterPreferences? = null,
    val lastNovelFilter: SearchFilterPreferences? = null,
) {
    fun lastFilter(mode: AppViewMode): SearchFilterPreferences? = when (mode) {
        AppViewMode.ILLUST -> lastIllustFilter
        AppViewMode.NOVEL -> lastNovelFilter
    }

    fun rememberFilter(mode: AppViewMode, filter: SearchFilterPreferences): SearchSettings =
        when (mode) {
            AppViewMode.ILLUST -> copy(lastIllustFilter = filter)
            AppViewMode.NOVEL -> copy(lastNovelFilter = filter)
        }

    /** 将明确选择的默认项同步到两种搜索模式，保留各自的其他筛选条件。 */
    fun updateFilterDefaults(
        transform: SearchFilterPreferences.() -> SearchFilterPreferences,
    ): SearchSettings {
        val defaults = SearchFilterPreferences(
            sort = defaultSearchSort,
            searchTarget = defaultSearchTarget,
            searchAiType = defaultSearchAiType,
            contentFilter = defaultContentFilter,
        ).transform()
        return copy(
            defaultSearchSort = defaults.sort,
            defaultSearchTarget = defaults.searchTarget,
            defaultSearchAiType = defaults.searchAiType,
            defaultContentFilter = defaults.contentFilter,
            lastIllustFilter = lastIllustFilter?.transform(),
            lastNovelFilter = lastNovelFilter?.transform(),
        )
    }
}

@Serializable
data class SearchFilterPreferences(
    val sort: SearchSort = SearchSort.POPULAR_DESC,
    val searchTarget: SearchTarget = SearchTarget.PARTIAL_MATCH_FOR_TAGS,
    val searchAiType: SearchAiType = SearchAiType.HIDE_AI,
    val contentFilter: SearchContentFilter = SearchContentFilter(),
    val illustAdvanced: IllustAdvancedFilter = IllustAdvancedFilter(),
    val novelAdvanced: NovelAdvancedFilter = NovelAdvancedFilter(),
    val bookmarkNumRange: SearchNumberRange? = null,
    val bookmarkStringRange: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)

@Serializable
enum class SearchResultDisplayMode {
    INFINITE_SCROLL,
    PAGED,
}
