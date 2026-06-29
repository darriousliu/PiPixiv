package com.mrl.pixiv.history

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.repository.BrowsingHistoryRepository
import com.mrl.pixiv.common.repository.paging.HistoryIllustPagingSource
import com.mrl.pixiv.common.repository.paging.HistoryNovelPagingSource
import com.mrl.pixiv.common.repository.paging.LocalHistoryIllustPagingSource
import com.mrl.pixiv.common.repository.paging.LocalHistoryNovelPagingSource
import com.mrl.pixiv.common.repository.requireUserInfoFlow
import com.mrl.pixiv.common.repository.requireUserInfoValue
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import kotlinx.coroutines.flow.SharingStarted
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

    val cloudIllusts = Pager(PagingConfig(pageSize = 20)) {
        get<HistoryIllustPagingSource>()
    }.flow.cachedIn(viewModelScope)

    val cloudNovels = Pager(PagingConfig(pageSize = 20)) {
        HistoryNovelPagingSource()
    }.flow.cachedIn(viewModelScope)

    val localIllusts = localIllustCount.flatMapLatest {
        Pager(PagingConfig(pageSize = 20)) {
            LocalHistoryIllustPagingSource(browsingHistoryRepository)
        }.flow
    }.cachedIn(viewModelScope)

    val localNovels = localNovelCount.flatMapLatest {
        Pager(PagingConfig(pageSize = 20)) {
            LocalHistoryNovelPagingSource(browsingHistoryRepository)
        }.flow
    }.cachedIn(viewModelScope)

    override suspend fun handleIntent(intent: HistoryAction) {
        when (intent) {
            is HistoryAction.UpdateSearch ->
                updateState { copy(currentSearch = intent.search) }

            is HistoryAction.UpdateMode ->
                updateState { copy(mode = intent.mode) }
        }
    }
}
