package com.mrl.pixiv.search.result

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.search.SearchIllustQuery
import com.mrl.pixiv.common.data.search.SearchNovelQuery
import com.mrl.pixiv.common.repository.SearchRepository
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.paging.SearchIllustPagingSource
import com.mrl.pixiv.common.repository.paging.SearchNovelPagingSource
import com.mrl.pixiv.common.repository.paging.SearchUserPagingSource
import com.mrl.pixiv.common.repository.requireUserInfoFlow
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.search.SearchState.SearchFilter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.format
import org.koin.android.annotation.KoinViewModel
import org.koin.core.component.KoinComponent


@Stable
data class SearchResultState(
    val searchWords: String = "",
    val bookmarkNumRange: IntRange? = null,
    val bookmarkStringRange: String? = null,
    val searchDateRange: LocalDateRange? = null,
    val searchFilter: SearchFilter = SearchFilter(),
)

sealed class SearchResultAction : ViewIntent {
    data class UpdateFilter(
        val searchFilter: SearchFilter,
    ) : SearchResultAction()

    data class UpdateBookmarkNumRange(
        val bookmarkNumRange: IntRange?,
    ) : SearchResultAction()

    data class UpdateSearchDateRange(
        val searchDateRange: LocalDateRange?,
    ) : SearchResultAction()
}

@KoinViewModel
class SearchResultViewModel(
    searchWords: String,
    private val searchMode: AppViewMode,
    private val isIdSearch: Boolean,
) : BaseMviViewModel<SearchResultState, SearchResultAction>(
    initialState = SearchResultState(
        searchWords = searchWords,
        searchFilter = if (SearchRepository.rememberSearchFilterValue) {
            val saved = SearchRepository.savedSearchFilterValue
            SearchFilter(
                sort = saved.sort,
                searchTarget = saved.searchTarget,
                searchAiType = saved.searchAiType,
            )
        } else {
            SearchFilter()
        }
    ),
), KoinComponent {
    val searchResults by lazy {
        combine(
            uiState,
            requireUserInfoFlow.map { it.profile.isPremium }.distinctUntilChanged()
        ) { state, isPremium ->
            state to isPremium
        }.flatMapLatest { (state, isPremium) ->
            val words = if (state.bookmarkStringRange != null) {
                "${state.searchWords} ${state.bookmarkStringRange}"
            } else {
                state.searchWords
            }
            val filter = state.searchFilter
            val startDate = state.searchDateRange?.start
            val endDate = state.searchDateRange?.endInclusive
            Pager(config = PagingConfig(pageSize = 20)) {
                SearchIllustPagingSource(
                    SearchIllustQuery(
                        word = words,
                        searchTarget = filter.searchTarget,
                        sort = filter.sort,
                        bookmarkNumMin = state.bookmarkNumRange?.start,
                        bookmarkNumMax = state.bookmarkNumRange?.endInclusive?.takeIf { it != Int.MAX_VALUE },
                        startDate = startDate?.format(LocalDate.Formats.ISO),
                        endDate = endDate?.format(LocalDate.Formats.ISO),
                        searchAiType = filter.searchAiType,
                    ),
                    isPremium = isPremium,
                    isIdSearch = isIdSearch
                )
            }.flow
        }.cachedIn(viewModelScope)
    }

    val novelSearchResults by lazy {
        combine(
            uiState,
            requireUserInfoFlow.map { it.profile.isPremium }.distinctUntilChanged()
        ) { state, isPremium ->
            state to isPremium
        }.flatMapLatest { (state, isPremium) ->
            val words = if (state.bookmarkStringRange != null) {
                "${state.searchWords} ${state.bookmarkStringRange}"
            } else {
                state.searchWords
            }
            val filter = state.searchFilter
            val startDate = state.searchDateRange?.start
            val endDate = state.searchDateRange?.endInclusive
            Pager(config = PagingConfig(pageSize = 20)) {
                SearchNovelPagingSource(
                    SearchNovelQuery(
                        word = words,
                        searchTarget = filter.searchTarget,
                        sort = filter.sort,
                        bookmarkNumMin = state.bookmarkNumRange?.start,
                        bookmarkNumMax = state.bookmarkNumRange?.endInclusive?.takeIf { it != Int.MAX_VALUE },
                        startDate = startDate?.format(LocalDate.Formats.ISO),
                        endDate = endDate?.format(LocalDate.Formats.ISO),
                    ),
                    isPremium = isPremium,
                    isIdSearch = isIdSearch
                )
            }.flow
        }.cachedIn(viewModelScope)
    }

    val userSearchResults = uiState.map { it.searchWords }
        .distinctUntilChanged()
        .flatMapLatest { words ->
            Pager(config = PagingConfig(pageSize = 20)) {
                SearchUserPagingSource(word = words, isIdSearch = isIdSearch)
            }.flow
        }.cachedIn(viewModelScope)


    override suspend fun handleIntent(intent: SearchResultAction) {
        when (intent) {
            is SearchResultAction.UpdateFilter ->
                updateState { copy(searchFilter = intent.searchFilter) }

            is SearchResultAction.UpdateBookmarkNumRange ->
                updateState {
                    copy(
                        bookmarkNumRange = intent.bookmarkNumRange,
                        bookmarkStringRange = null
                    )
                }

            is SearchResultAction.UpdateSearchDateRange ->
                updateState { copy(searchDateRange = intent.searchDateRange) }
        }
    }

    fun updateBookmarkStringRange(range: String?) {
        updateState { copy(bookmarkStringRange = range, bookmarkNumRange = null) }
    }

    fun switchViewMode(mode: AppViewMode) {
        SettingRepository.setAppViewMode(mode)
    }
}
