package com.mrl.pixiv.common.data.search

import kotlinx.serialization.Serializable

@Serializable
data class Search(
    val searchHistoryList: List<SearchHistory> = emptyList(),
)

@Serializable
data class SearchHistory(
    val keyword: String,
    val timestamp: Long,
)

@Serializable
data class LocalSearchFilter(
    val sort: SearchSort = SearchSort.POPULAR_DESC,
    val searchTarget: SearchTarget = SearchTarget.PARTIAL_MATCH_FOR_TAGS,
    val searchAiType: SearchAiType = SearchAiType.HIDE_AI,
)
