package com.mrl.pixiv.history

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.repository.BrowsingHistoryRepository
import com.mrl.pixiv.common.repository.paging.HistoryIllustPagingSource
import com.mrl.pixiv.common.repository.paging.HistoryNovelPagingSource
import com.mrl.pixiv.common.repository.paging.LocalHistoryIllustPagingSource
import com.mrl.pixiv.common.repository.paging.LocalHistoryNovelPagingSource
import com.mrl.pixiv.common.repository.requireUserInfoFlow
import com.mrl.pixiv.common.repository.requireUserInfoValue
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

data class HistoryState(
    val currentSearch: String = "",
    val mode: AppViewMode = AppViewMode.ILLUST,
)

sealed class HistoryAction : ViewIntent {
    data class UpdateSearch(val search: String) : HistoryAction()
    data class UpdateMode(val mode: AppViewMode) : HistoryAction()
}

@KoinViewModel
class HistoryViewModel(
    private val browsingHistoryRepository: BrowsingHistoryRepository,
) : BaseMviViewModel<HistoryState, HistoryAction>(
    initialState = HistoryState(),
), KoinComponent {
    val userPreferenceFlow = browsingHistoryRepository.userPreferenceFlow
    private val searchFlow = uiState
        .map { it.currentSearch }
        .distinctUntilChanged()

    val isPremiumFlow = requireUserInfoFlow
        .map { it.profile.isPremium }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = requireUserInfoValue.profile.isPremium,
        )

    val localIllustCount = browsingHistoryRepository.observeLocalIllustCount()

    val localNovelCount = browsingHistoryRepository.observeLocalNovelCount()

    val localIllustGridState = LazyGridState()
    val cloudIllustGridState = LazyGridState()
    val localNovelListState = LazyListState()
    val cloudNovelListState = LazyListState()

    private val cloudIllustsSource = Pager(PagingConfig(pageSize = 20)) {
        get<HistoryIllustPagingSource>()
    }.flow

    private val cloudNovelsSource = Pager(PagingConfig(pageSize = 20)) {
        HistoryNovelPagingSource()
    }.flow

    private val localIllustsSource = localIllustCount.flatMapLatest {
        Pager(PagingConfig(pageSize = 20)) {
            LocalHistoryIllustPagingSource(browsingHistoryRepository)
        }.flow
    }

    private val localNovelsSource = localNovelCount.flatMapLatest {
        Pager(PagingConfig(pageSize = 20)) {
            LocalHistoryNovelPagingSource(browsingHistoryRepository)
        }.flow
    }

    val cloudIllusts = cloudIllustsSource
        .filterIllustsBySearch()
        .cachedIn(viewModelScope)

    val cloudNovels = cloudNovelsSource
        .filterNovelsBySearch()
        .cachedIn(viewModelScope)

    val localIllusts = localIllustsSource
        .filterIllustsBySearch()
        .cachedIn(viewModelScope)

    val localNovels = localNovelsSource
        .filterNovelsBySearch()
        .cachedIn(viewModelScope)

    private fun Flow<PagingData<Illust>>.filterIllustsBySearch(): Flow<PagingData<Illust>> =
        combine(searchFlow) { pagingData, search ->
            pagingData.filter { it.matches(search) }
        }

    private fun Flow<PagingData<Novel>>.filterNovelsBySearch(): Flow<PagingData<Novel>> =
        combine(searchFlow) { pagingData, search ->
            pagingData.filter { it.matches(search) }
        }

    override suspend fun handleIntent(intent: HistoryAction) {
        when (intent) {
            is HistoryAction.UpdateSearch ->
                updateState { copy(currentSearch = intent.search) }

            is HistoryAction.UpdateMode ->
                updateState { copy(mode = intent.mode) }
        }
    }
}

private fun Illust.matches(search: String): Boolean {
    if (search.isBlank()) return true
    return title.contains(search, ignoreCase = true) ||
            user.name.contains(search, ignoreCase = true)
}

private fun Novel.matches(search: String): Boolean {
    if (search.isBlank()) return true
    return title.contains(search, ignoreCase = true) ||
            user.name.contains(search, ignoreCase = true)
}
