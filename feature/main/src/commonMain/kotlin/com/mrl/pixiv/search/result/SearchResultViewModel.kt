package com.mrl.pixiv.search.result

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.search.SearchIllustQuery
import com.mrl.pixiv.common.data.search.SearchNovelQuery
import com.mrl.pixiv.common.data.search.SearchSort
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.feed.PagedFeedController
import com.mrl.pixiv.common.repository.feed.SearchIllustFeedSource
import com.mrl.pixiv.common.repository.feed.SearchNovelFeedSource
import com.mrl.pixiv.common.repository.feed.SearchUserFeedSource
import com.mrl.pixiv.common.repository.paging.SearchIllustPagingSource
import com.mrl.pixiv.common.repository.paging.SearchNovelPagingSource
import com.mrl.pixiv.common.repository.paging.SearchUserPagingSource
import com.mrl.pixiv.common.repository.requireUserInfoFlow
import com.mrl.pixiv.common.repository.requireUserInfoValue
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.search.SearchState.SearchFilter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
        searchFilter = resolveInitialSearchFilter(
            searchSettings = SettingRepository.userPreferenceFlow.value.searchSettings,
            searchMode = searchMode,
        ),
    ),
), KoinComponent {
    private var manualIsPremium = requireUserInfoValue.profile.isPremium

    private val manualIllustController by lazy {
        PagedFeedController(viewModelScope) {
            SearchIllustFeedSource(
                query = currentIllustQuery(),
                isPremium = manualIsPremium,
                isIdSearch = isIdSearch,
            )
        }
    }
    private val manualNovelController by lazy {
        PagedFeedController(viewModelScope) {
            SearchNovelFeedSource(
                query = currentNovelQuery(),
                isPremium = manualIsPremium,
                isIdSearch = isIdSearch,
            )
        }
    }
    private val manualUserController by lazy {
        PagedFeedController(viewModelScope) {
            SearchUserFeedSource(
                word = uiState.value.searchWords,
                isIdSearch = isIdSearch,
            )
        }
    }

    val manualIllustResults by lazy { manualIllustController.state }
    val manualNovelResults by lazy { manualNovelController.state }
    val manualUserResults by lazy { manualUserController.state }

    val isPremium = requireUserInfoFlow
        .map { it.profile.isPremium }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = manualIsPremium,
        )

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
                        searchAiType = filter.searchAiType,
                    ),
                    isPremium = isPremium,
                    isIdSearch = isIdSearch
                )
            }.flow
        }.cachedIn(viewModelScope)
    }

    val userSearchResults by lazy {
        uiState.map { it.searchWords }
            .distinctUntilChanged()
            .flatMapLatest { words ->
                Pager(config = PagingConfig(pageSize = 20)) {
                    SearchUserPagingSource(word = words, isIdSearch = isIdSearch)
                }.flow
            }.cachedIn(viewModelScope)
    }


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

    fun ensureManualPage(page: SearchResultsPage, isPremium: Boolean) {
        manualIsPremium = isPremium
        when (page) {
            SearchResultsPage.IllustsOrNovel -> {
                when (searchMode) {
                    AppViewMode.ILLUST -> {
                        val query = currentIllustQuery()
                        manualIllustController.ensureLoaded(
                            ManualIllustQueryKey(
                                query = query,
                                isPremium = isPremium,
                                isIdSearch = isIdSearch,
                            )
                        )
                    }

                    AppViewMode.NOVEL -> {
                        val query = currentNovelQuery()
                        manualNovelController.ensureLoaded(
                            ManualNovelQueryKey(
                                query = query,
                                isPremium = isPremium,
                                isIdSearch = isIdSearch,
                            )
                        )
                    }
                }
            }

            SearchResultsPage.Users -> {
                manualUserController.ensureLoaded(
                    ManualUserQueryKey(
                        word = uiState.value.searchWords,
                        isIdSearch = isIdSearch,
                    )
                )
            }
        }
    }

    fun isPopularPreview(isPremium: Boolean): Boolean {
        return !isIdSearch &&
            !isPremium &&
            uiState.value.searchFilter.sort == SearchSort.POPULAR_DESC
    }

    fun switchToLatestSort() {
        val latestFilter = uiState.value.searchFilter.copy(sort = SearchSort.DATE_DESC)
        dispatch(
            SearchResultAction.UpdateFilter(latestFilter)
        )
    }

    fun refreshManualPage(page: SearchResultsPage) {
        when (page) {
            SearchResultsPage.IllustsOrNovel -> {
                when (searchMode) {
                    AppViewMode.ILLUST -> manualIllustController.refresh()
                    AppViewMode.NOVEL -> manualNovelController.refresh()
                }
            }

            SearchResultsPage.Users -> manualUserController.refresh()
        }
    }

    fun loadPreviousManualPage(page: SearchResultsPage) {
        when (page) {
            SearchResultsPage.IllustsOrNovel -> {
                when (searchMode) {
                    AppViewMode.ILLUST -> manualIllustController.previousPage()
                    AppViewMode.NOVEL -> manualNovelController.previousPage()
                }
            }

            SearchResultsPage.Users -> manualUserController.previousPage()
        }
    }

    fun loadNextManualPage(page: SearchResultsPage) {
        when (page) {
            SearchResultsPage.IllustsOrNovel -> {
                when (searchMode) {
                    AppViewMode.ILLUST -> manualIllustController.nextPage()
                    AppViewMode.NOVEL -> manualNovelController.nextPage()
                }
            }

            SearchResultsPage.Users -> manualUserController.nextPage()
        }
    }

    fun loadManualPage(page: SearchResultsPage, pageNumber: Int) {
        when (page) {
            SearchResultsPage.IllustsOrNovel -> {
                when (searchMode) {
                    AppViewMode.ILLUST -> manualIllustController.loadPage(pageNumber)
                    AppViewMode.NOVEL -> manualNovelController.loadPage(pageNumber)
                }
            }

            SearchResultsPage.Users -> manualUserController.loadPage(pageNumber)
        }
    }

    private fun currentIllustQuery(): SearchIllustQuery {
        val state = uiState.value
        val filter = state.searchFilter
        val startDate = state.searchDateRange?.start
        val endDate = state.searchDateRange?.endInclusive
        return SearchIllustQuery(
            word = state.searchWordsWithBookmarkRange(),
            searchTarget = filter.searchTarget,
            sort = filter.sort,
            bookmarkNumMin = state.bookmarkNumRange?.start,
            bookmarkNumMax = state.bookmarkNumRange?.endInclusive?.takeIf { it != Int.MAX_VALUE },
            startDate = startDate?.format(LocalDate.Formats.ISO),
            endDate = endDate?.format(LocalDate.Formats.ISO),
            searchAiType = filter.searchAiType,
        )
    }

    private fun currentNovelQuery(): SearchNovelQuery {
        val state = uiState.value
        val filter = state.searchFilter
        val startDate = state.searchDateRange?.start
        val endDate = state.searchDateRange?.endInclusive
        return SearchNovelQuery(
            word = state.searchWordsWithBookmarkRange(),
            searchTarget = filter.searchTarget,
            sort = filter.sort,
            bookmarkNumMin = state.bookmarkNumRange?.start,
            bookmarkNumMax = state.bookmarkNumRange?.endInclusive?.takeIf { it != Int.MAX_VALUE },
            startDate = startDate?.format(LocalDate.Formats.ISO),
            endDate = endDate?.format(LocalDate.Formats.ISO),
            searchAiType = filter.searchAiType,
        )
    }

    private fun SearchResultState.searchWordsWithBookmarkRange(): String {
        return bookmarkStringRange?.let { "$searchWords $it" } ?: searchWords
    }
}

private data class ManualIllustQueryKey(
    val query: SearchIllustQuery,
    val isPremium: Boolean,
    val isIdSearch: Boolean,
)

private data class ManualNovelQueryKey(
    val query: SearchNovelQuery,
    val isPremium: Boolean,
    val isIdSearch: Boolean,
)

private data class ManualUserQueryKey(
    val word: String,
    val isIdSearch: Boolean,
)
