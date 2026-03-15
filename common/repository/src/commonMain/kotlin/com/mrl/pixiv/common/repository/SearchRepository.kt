package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.search.IllustSearch
import com.mrl.pixiv.common.data.search.LocalSearchFilter
import com.mrl.pixiv.common.data.search.NovelSearch
import com.mrl.pixiv.common.data.search.SearchHistory
import com.mrl.pixiv.common.mmkv.MMKVUser
import com.mrl.pixiv.common.mmkv.asMutableStateFlow
import com.mrl.pixiv.common.mmkv.mmkvBool
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import com.mrl.pixiv.common.mmkv.mmkvStringSet
import com.mrl.pixiv.common.util.currentTimeMillis
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object SearchRepository : MMKVUser {
    //------------------------------- 插图历史记录 ---------------------------------
    private val searchHistory by mmkvSerializable(IllustSearch()).asMutableStateFlow()
    val searchHistoryFlow = searchHistory.asStateFlow()

    private val searchIdHistory by mmkvStringSet(emptySet()).asMutableStateFlow()
    val searchIdHistoryFlow = searchIdHistory.asStateFlow()

    //------------------------------- 小说历史记录 ---------------------------------
    private val novelSearchHistory by mmkvSerializable(NovelSearch()).asMutableStateFlow()
    val novelSearchHistoryFlow = novelSearchHistory.asStateFlow()

    private val novelSearchIdHistory by mmkvStringSet(emptySet()).asMutableStateFlow()
    val novelSearchIdHistoryFlow = novelSearchIdHistory.asStateFlow()

    private val _savedSearchFilter by mmkvSerializable(LocalSearchFilter()).asMutableStateFlow()
    val savedSearchFilterFlow = _savedSearchFilter.asStateFlow()

    private val _rememberSearchFilter by mmkvBool(false).asMutableStateFlow()
    val rememberSearchFilterFlow = _rememberSearchFilter.asStateFlow()

    val rememberSearchFilterValue: Boolean
        get() = _rememberSearchFilter.value

    val savedSearchFilterValue: LocalSearchFilter
        get() = _savedSearchFilter.value

    fun setRememberSearchFilter(remember: Boolean) {
        _rememberSearchFilter.value = remember
    }

    fun setSavedSearchFilter(filter: LocalSearchFilter) {
        _savedSearchFilter.value = filter
    }

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
                                timestamp = currentTimeMillis()
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

    fun deleteNovelSearchHistory(searchWords: String) {
        novelSearchHistory.update {
            val index = it.novelSearchHistory.indexOfFirst { it.keyword == searchWords }
            it.copy(
                novelSearchHistory = it.novelSearchHistory.toMutableList().apply {
                    removeAt(index)
                }
            )
        }
    }

    fun addNovelSearchHistory(searchWords: String) {
        novelSearchHistory.update {
            // add to search history if not exist
            val index = it.novelSearchHistory.indexOfFirst { it.keyword == searchWords }
            if (index == -1) {
                it.copy(
                    novelSearchHistory = it.novelSearchHistory.toMutableList().apply {
                        add(
                            0, SearchHistory(
                                keyword = searchWords,
                                timestamp = currentTimeMillis()
                            )
                        )
                    }
                )
            } else {
                // move to first if exist
                val searchHistory = it.novelSearchHistory[index]
                it.copy(
                    novelSearchHistory = it.novelSearchHistory.toMutableList().apply {
                        removeAt(index)
                        add(0, searchHistory)
                    }
                )
            }
        }
    }

    fun addSearchIdHistory(searchId: String) {
        searchIdHistory.update { (it ?: emptySet()) + searchId }
    }

    fun removeSearchIdHistory(searchId: String) {
        searchIdHistory.update { it?.minus(searchId) }
    }

    fun addNovelSearchIdHistory(searchId: String) {
        novelSearchIdHistory.update { (it ?: emptySet()) + searchId }
    }

    fun removeNovelSearchIdHistory(searchId: String) {
        novelSearchIdHistory.update { it?.minus(searchId) }
    }

    fun clear() {
        searchHistory.update {
            it.copy(searchHistoryList = emptyList())
        }
        searchIdHistory.update { emptySet() }
    }

    fun restore(
        illustSearch: IllustSearch,
        searchIds: Set<String>,
        novelSearch: NovelSearch,
        novelSearchIds: Set<String>,
        savedFilter: LocalSearchFilter? = null,
        rememberFilter: Boolean? = null
    ) {
        searchHistory.value = illustSearch
        searchIdHistory.value = searchIds
        novelSearchHistory.value = novelSearch
        novelSearchIdHistory.value = novelSearchIds
        if (savedFilter != null) {
            _savedSearchFilter.value = savedFilter
        }
        if (rememberFilter != null) {
            _rememberSearchFilter.value = rememberFilter
        }
    }
}