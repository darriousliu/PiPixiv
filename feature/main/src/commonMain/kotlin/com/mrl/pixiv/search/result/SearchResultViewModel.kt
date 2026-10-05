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
import com.mrl.pixiv.common.data.search.SearchOptionsResponse
import com.mrl.pixiv.common.data.search.SearchTarget
import com.mrl.pixiv.common.data.search.SearchAiType
import com.mrl.pixiv.common.data.search.validatedBy
import com.mrl.pixiv.common.repository.PixivRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.feed.PagedFeedController
import com.mrl.pixiv.common.repository.feed.SearchIllustFeedSource
import com.mrl.pixiv.common.repository.feed.SearchNovelFeedSource
import com.mrl.pixiv.common.repository.feed.SearchUserFeedSource
import com.mrl.pixiv.common.repository.hasDifferentNovelFilterSettings
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
import kotlinx.coroutines.launch
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
            defaultShowR18 = SettingRepository.userPreferenceFlow.value.isR18Enabled,
        ),
    ),
), KoinComponent {
    private val _searchOptions = MutableStateFlow(SearchOptionsState())
    val searchOptions = _searchOptions.asStateFlow()
    private var optionsJob: Job? = null

    fun loadSearchOptions(target: SearchTarget, ai: SearchAiType) {
        optionsJob?.cancel()
        optionsJob = viewModelScope.launch {
            _searchOptions.value = SearchOptionsState(loading = true)
            try {
                val result = PixivRepository.getSearchOptions(uiState.value.searchWords, target.value, ai.value)
                _searchOptions.value = SearchOptionsState(options = result)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _searchOptions.value = SearchOptionsState(failed = true)
            }
        }
    }

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
    private val manualNovelControllerDelegate = lazy {
        PagedFeedController(viewModelScope) {
            SearchNovelFeedSource(
                query = currentNovelQuery(),
                isPremium = manualIsPremium,
                isIdSearch = isIdSearch,
            )
        }
    }
    private val manualNovelController by manualNovelControllerDelegate
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

    private var observedNovelFilterSettings =
        SettingRepository.userPreferenceFlow.value.browsingSettings

    init {
        if (searchMode == AppViewMode.NOVEL) {
            viewModelScope.launch {
                SettingRepository.userPreferenceFlow
                    .map { it.browsingSettings }
                    .collect { currentSettings ->
                        val shouldRefresh = observedNovelFilterSettings
                            .hasDifferentNovelFilterSettings(currentSettings)
                        observedNovelFilterSettings = currentSettings
                        if (shouldRefresh && manualNovelControllerDelegate.isInitialized()) {
                            manualNovelController.refresh()
                        }
                    }
            }
        }
    }

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
                        contentFilter = filter.contentFilter,
            advanced = filter.illustAdvanced,
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
                        contentFilter = filter.contentFilter,
            advanced = filter.novelAdvanced,
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
            is SearchResultAction.UpdateFilter -> {
                val selected = intent.searchFilter
                val options = searchOptions.value.options
                val validated = if (options == null) selected else selected.copy(
                    illustAdvanced = selected.illustAdvanced.validatedBy(options),
                    novelAdvanced = selected.novelAdvanced.validatedBy(options),
                )
                val active = if (searchMode == AppViewMode.NOVEL) validated.novelAdvanced.isActive
                    else validated.illustAdvanced.isActive
                val filter = if (!isPremium.value && active && validated.sort == SearchSort.POPULAR_DESC)
                    validated.copy(sort = SearchSort.DATE_DESC) else validated
                updateState { copy(searchFilter = filter) }
            }

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
            uiState.value.searchFilter.sort == SearchSort.POPULAR_DESC &&
            when (searchMode) {
                AppViewMode.ILLUST -> !uiState.value.searchFilter.illustAdvanced.isActive
                AppViewMode.NOVEL -> !uiState.value.searchFilter.novelAdvanced.isActive
            }
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
            contentFilter = filter.contentFilter,
            advanced = filter.illustAdvanced,
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
            contentFilter = filter.contentFilter,
            advanced = filter.novelAdvanced,
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


data class SearchOptionsState(
    val options: SearchOptionsResponse? = null,
    val loading: Boolean = false,
    val failed: Boolean = false,
)
