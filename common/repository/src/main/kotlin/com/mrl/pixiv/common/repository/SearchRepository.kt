package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.search.Search
import com.mrl.pixiv.common.data.search.SearchHistory
import com.mrl.pixiv.common.mmkv.MMKVUser
import com.mrl.pixiv.common.mmkv.asMutableStateFlow
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object SearchRepository : MMKVUser {
    private val searchHistory by mmkvSerializable(Search()).asMutableStateFlow()
    val searchHistoryFlow = searchHistory.asStateFlow()

    fun deleteSearchHistory(searchWords: String) {
        searchHistory.update {
            val index = it.searchHistoryList.indexOfFirst { it.keyword == searchWords }
            it.copy(
                searchHistoryList = it.searchHistoryList.toMutableList().apply {
                    removeAt(index)
                }
            )
        }
    }

    fun addSearchHistory(searchWords: String) {
        searchHistory.update {
            // add to search history if not exist
            val index = it.searchHistoryList.indexOfFirst { it.keyword == searchWords }
            if (index == -1) {
                it.copy(
                    searchHistoryList = it.searchHistoryList.toMutableList().apply {
                        add(
                            0, SearchHistory(
                                keyword = searchWords,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                )
            } else {
                // move to first if exist
                val searchHistory = it.searchHistoryList[index]
                it.copy(
                    searchHistoryList = it.searchHistoryList.toMutableList().apply {
                        removeAt(index)
                        add(0, searchHistory)
                    }
                )
            }
        }
    }

    fun clear() {
        searchHistory.update {
            it.copy(
                searchHistoryList = emptyList()
            )
        }
    }
}