@file:Suppress("DEPRECATION")

package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.search.IllustSearch
import com.mrl.pixiv.common.data.search.LocalSearchFilter
import com.mrl.pixiv.common.data.search.NovelSearch
import com.mrl.pixiv.common.data.search.SearchHistory
import com.mrl.pixiv.common.mmkv.AccountMMKVProperty
import com.mrl.pixiv.common.mmkv.AccountScoped
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.mmkv.MMKVUser
import com.mrl.pixiv.common.mmkv.mmkvBool
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import com.mrl.pixiv.common.mmkv.mmkvStringSet
import com.mrl.pixiv.common.mmkv.withAccountStorageLock
import com.mrl.pixiv.common.util.currentTimeMillis

object SearchRepository : MMKVUser, AccountScoped {
    //------------------------------- 插图历史记录 ---------------------------------
    private val searchHistory = AccountMMKVProperty(
        mmkvSerializable(IllustSearch()), "searchHistory", IllustSearch(),
        loadOnAccess = true,
    )
    val searchHistoryFlow = searchHistory.flow

    private val searchIdHistory = AccountMMKVProperty(
        mmkvStringSet(emptySet()), "searchIdHistory", emptySet(),
        loadOnAccess = true,
    )
    val searchIdHistoryFlow = searchIdHistory.flow

    //------------------------------- 小说历史记录 ---------------------------------
    private val novelSearchHistory = AccountMMKVProperty(
        mmkvSerializable(NovelSearch()), "novelSearchHistory", NovelSearch(),
        loadOnAccess = true,
    )
    val novelSearchHistoryFlow = novelSearchHistory.flow

    private val novelSearchIdHistory = AccountMMKVProperty(
        mmkvStringSet(emptySet()), "novelSearchIdHistory", emptySet(),
        loadOnAccess = true,
    )
    val novelSearchIdHistoryFlow = novelSearchIdHistory.flow

    @Deprecated("Persistent search filter defaults are stored in UserPreference.searchSettings.")
    private val _savedSearchFilter = AccountMMKVProperty(
        mmkvSerializable(LocalSearchFilter()), "_savedSearchFilter", LocalSearchFilter(),
    )

    @Deprecated("Use SettingRepository.userPreferenceFlow and SearchSettings.")
    val savedSearchFilterFlow
        get() = _savedSearchFilter.flow

    @Deprecated("Persistent search filter defaults are stored in UserPreference.searchSettings.")
    private val _rememberSearchFilter = AccountMMKVProperty(
        mmkvBool(false), "_rememberSearchFilter", false,
    )

    @Deprecated("Search filter changes are no longer remembered.")
    val rememberSearchFilterFlow
        get() = _rememberSearchFilter.flow

    @Deprecated("Search filter changes are no longer remembered.")
    val rememberSearchFilterValue: Boolean
        get() = _rememberSearchFilter.flow.value

    @Deprecated("Use SettingRepository.userPreferenceFlow and SearchSettings.")
    val savedSearchFilterValue: LocalSearchFilter
        get() = _savedSearchFilter.flow.value

    @Deprecated("Search filter changes are no longer remembered.")
    fun setRememberSearchFilter(remember: Boolean) {
        _rememberSearchFilter.write(AccountStore.activeUserId, remember)
    }

    @Deprecated("Use SettingRepository.setSearchSettings instead.")
    fun setSavedSearchFilter(filter: LocalSearchFilter) {
        _savedSearchFilter.write(AccountStore.activeUserId, filter)
    }

    fun deleteSearchHistory(searchWords: String) {
        searchHistory.update(AccountStore.activeUserId) {
            val index = it.searchHistoryList.indexOfFirst { it.keyword == searchWords }
            it.copy(
                searchHistoryList = it.searchHistoryList.toMutableList().apply {
                    removeAt(index)
                }
            )
        }
    }

    fun addSearchHistory(searchWords: String) {
        searchHistory.update(AccountStore.activeUserId) {
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
        novelSearchHistory.update(AccountStore.activeUserId) {
            val index = it.novelSearchHistory.indexOfFirst { it.keyword == searchWords }
            it.copy(
                novelSearchHistory = it.novelSearchHistory.toMutableList().apply {
                    removeAt(index)
                }
            )
        }
    }

    fun addNovelSearchHistory(searchWords: String) {
        novelSearchHistory.update(AccountStore.activeUserId) {
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
        searchIdHistory.update(AccountStore.activeUserId) { (it ?: emptySet()) + searchId }
    }

    fun removeSearchIdHistory(searchId: String) {
        searchIdHistory.update(AccountStore.activeUserId) { it?.minus(searchId) }
    }

    fun addNovelSearchIdHistory(searchId: String) {
        novelSearchIdHistory.update(AccountStore.activeUserId) { (it ?: emptySet()) + searchId }
    }

    fun removeNovelSearchIdHistory(searchId: String) {
        novelSearchIdHistory.update(AccountStore.activeUserId) { it?.minus(searchId) }
    }

    override fun reload() = withAccountStorageLock {
        val userId = AccountStore.activeUserId
        searchHistory.reload(userId)
        searchIdHistory.reload(userId)
        novelSearchHistory.reload(userId)
        novelSearchIdHistory.reload(userId)
        _savedSearchFilter.reload(userId)
        _rememberSearchFilter.reload(userId)
    }

    internal fun prefetch(userId: Long) {
        searchHistory.prefetch(userId)
        searchIdHistory.prefetch(userId)
        novelSearchHistory.prefetch(userId)
        novelSearchIdHistory.prefetch(userId)
    }

    fun historyForAccount(userId: Long): AccountSearchHistory = withAccountStorageLock {
        AccountSearchHistory(
            illustSearch = searchHistory.read(userId),
            illustSearchIds = searchIdHistory.read(userId).orEmpty(),
            novelSearch = novelSearchHistory.read(userId),
            novelSearchIds = novelSearchIdHistory.read(userId).orEmpty(),
            savedFilter = _savedSearchFilter.read(userId),
            rememberFilter = _rememberSearchFilter.read(userId),
        )
    }

    fun restore(
        illustSearch: IllustSearch,
        searchIds: Set<String>,
        novelSearch: NovelSearch,
        novelSearchIds: Set<String>,
        userId: Long = AccountStore.activeUserId,
        savedFilter: LocalSearchFilter? = null,
        rememberFilter: Boolean? = null,
    ) = withAccountStorageLock {
        searchHistory.write(userId, illustSearch)
        searchIdHistory.write(userId, searchIds)
        novelSearchHistory.write(userId, novelSearch)
        novelSearchIdHistory.write(userId, novelSearchIds)
        savedFilter?.let { _savedSearchFilter.write(userId, it) }
        rememberFilter?.let { _rememberSearchFilter.write(userId, it) }
    }
}

data class AccountSearchHistory(
    val illustSearch: IllustSearch,
    val illustSearchIds: Set<String>,
    val novelSearch: NovelSearch,
    val novelSearchIds: Set<String>,
    val savedFilter: LocalSearchFilter,
    val rememberFilter: Boolean,
)
