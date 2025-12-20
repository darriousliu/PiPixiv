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
