package com.mrl.pixiv.common.data.setting

import com.mrl.pixiv.common.data.search.SearchAiType
import com.mrl.pixiv.common.data.search.SearchSort
import com.mrl.pixiv.common.data.search.SearchTarget
import kotlinx.serialization.Serializable

@Serializable
data class SearchSettings(
    val defaultSearchTarget: SearchTarget = SearchTarget.PARTIAL_MATCH_FOR_TAGS,
    val defaultSearchSort: SearchSort = SearchSort.POPULAR_DESC,
    val defaultSearchAiType: SearchAiType = SearchAiType.HIDE_AI,
    val searchResultDisplayMode: SearchResultDisplayMode = SearchResultDisplayMode.INFINITE_SCROLL,
)

@Serializable
enum class SearchResultDisplayMode {
    INFINITE_SCROLL,
    PAGED,
}
