package com.mrl.pixiv.latest

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.pager.PagerState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.repository.paging.IllustFollowingPagingSource
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class LatestViewModel : ViewModel() {
    val pagerState = PagerState { LatestPage.entries.size }
    val trendingLazyGirdState = LazyStaggeredGridState()
    val collectionLazyGirdState = LazyStaggeredGridState()
    val followingLazyListState = LazyListState()
    val followingLazyGirdState = LazyGridState()

    val trendingFilter = MutableStateFlow(Restrict.ALL)

    val illustsFollowing = Pager(PagingConfig(pageSize = 20)) {
        IllustFollowingPagingSource(restrict = trendingFilter.value)
    }.flow.cachedIn(viewModelScope)

    fun updateRestrict(restrict: Restrict) {
        trendingFilter.value = restrict
    }
}