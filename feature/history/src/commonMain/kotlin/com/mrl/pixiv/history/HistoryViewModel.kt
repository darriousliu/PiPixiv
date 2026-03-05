package com.mrl.pixiv.history

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.repository.paging.HistoryIllustPagingSource
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import org.koin.android.annotation.KoinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

data class HistoryState(
    val currentSearch: String = "",
)

sealed class HistoryAction : ViewIntent {
    data class UpdateSearch(val search: String) : HistoryAction()
}

@KoinViewModel
class HistoryViewModel : BaseMviViewModel<HistoryState, HistoryAction>(
    initialState = HistoryState(),
), KoinComponent {
    val illusts = Pager(PagingConfig(pageSize = 20)) {
        get<HistoryIllustPagingSource>()
    }.flow.cachedIn(viewModelScope)

    override suspend fun handleIntent(intent: HistoryAction) {
        when (intent) {
            is HistoryAction.UpdateSearch ->
                updateState { copy(currentSearch = intent.search) }
        }
    }
}