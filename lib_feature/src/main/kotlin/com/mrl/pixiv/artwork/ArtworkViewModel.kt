package com.mrl.pixiv.artwork

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.repository.paging.UserIllustPagingSource
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class ArtworkViewModel(
    private val userId: Long,
) : BaseMviViewModel<Any, ViewIntent>(
    initialState = Any()
) {
    val userIllusts = Pager(PagingConfig(pageSize = 20)) {
        UserIllustPagingSource(userId)
    }.flow.cachedIn(viewModelScope)

    override suspend fun handleIntent(intent: ViewIntent) {

    }
}