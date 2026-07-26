package com.mrl.pixiv.common.data.setting

import com.mrl.pixiv.common.data.search.SearchSort
import kotlinx.serialization.Serializable

@Serializable
data class SearchSettings(
    val defaultSearchSort: SearchSort = SearchSort.POPULAR_DESC,
    val searchResultDisplayMode: SearchResultDisplayMode = SearchResultDisplayMode.INFINITE_SCROLL,
)

@Serializable
enum class SearchResultDisplayMode {
    INFINITE_SCROLL,
    PAGED,
}
