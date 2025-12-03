package com.mrl.pixiv.search.result

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.search.SearchIllustQuery
import com.mrl.pixiv.common.repository.paging.SearchIllustPagingSource
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.search.SearchState.SearchFilter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.format
import org.koin.android.annotation.KoinViewModel
import org.koin.core.component.KoinComponent


@Stable
data class SearchResultState(
    val searchWords: String = "",
    val bookmarkNumRange: IntRange? = null,
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
) : BaseMviViewModel<SearchResultState, SearchResultAction>(
    initialState = SearchResultState(searchWords = searchWords),
), KoinComponent {
    val searchResults = uiState.flatMapLatest { state ->
        val words = state.searchWords
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
                )
            )
        }.flow.cachedIn(viewModelScope)
    }

    override suspend fun handleIntent(intent: SearchResultAction) {
        when (intent) {
            is SearchResultAction.UpdateFilter ->
                updateState { copy(searchFilter = intent.searchFilter) }

            is SearchResultAction.UpdateBookmarkNumRange ->
                updateState { copy(bookmarkNumRange = intent.bookmarkNumRange) }

            is SearchResultAction.UpdateSearchDateRange ->
                updateState { copy(searchDateRange = intent.searchDateRange) }
        }
    }
}
