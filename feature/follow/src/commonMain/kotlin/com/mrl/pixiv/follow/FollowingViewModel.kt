package com.mrl.pixiv.follow

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.PagerState
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.repository.isSelf
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

    val pages = if (uid.isSelf) {
        listOf(FollowingPage.Public, FollowingPage.Private)
    } else {
        listOf(FollowingPage.Public)
    }

    val pagerState = PagerState { pages.size }
    val lazyListState = List(pagerState.pageCount) { LazyListState() }
    val lazyGridState = List(pagerState.pageCount) { LazyGridState() }

    override suspend fun handleIntent(intent: FollowingAction) {

    }
}