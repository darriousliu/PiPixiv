package com.mrl.pixiv.follow

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.repository.paging.FollowingPagingSource
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class FollowingViewModel(
    private val uid: Long,
) : BaseMviViewModel<FollowingState, FollowingAction>(
    initialState = FollowingState
) {
    val publicFollowingPageSource = Pager(PagingConfig(pageSize = 30)) {
        FollowingPagingSource(uid, Restrict.PUBLIC)
    }.flow.cachedIn(viewModelScope)
    val privateFollowingPageSource = Pager(PagingConfig(pageSize = 30)) {
        FollowingPagingSource(uid, Restrict.PRIVATE)
    }.flow.cachedIn(viewModelScope)

    override suspend fun handleIntent(intent: FollowingAction) {

    }
}