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
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.repository.NovelWatchlistChanges
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.paging.FollowNovelPagingSource
import com.mrl.pixiv.common.repository.paging.IllustFollowingPagingSource
import com.mrl.pixiv.common.repository.paging.NovelNewPagingSource
import com.mrl.pixiv.common.repository.paging.NovelWatchlistPagingSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class LatestViewModel : ViewModel() {
    private var activeNovelWatchlistSource: NovelWatchlistPagingSource? = null
    private val illustPagerState = PagerState {
        LatestPage.pagesFor(AppViewMode.ILLUST).size
    }
    private val novelPagerState = PagerState {
        LatestPage.pagesFor(AppViewMode.NOVEL).size
    }

    // Illust states
    val trendingLazyGirdState = LazyStaggeredGridState()
    val collectionLazyGirdState = LazyStaggeredGridState()
    val followingLazyListState = LazyListState()
    val followingLazyGirdState = LazyGridState()

    // Novel states
    val trendingNovelLazyListState = LazyListState()
    val newNovelLazyListState = LazyListState()
    val watchlistNovelLazyListState = LazyListState()
    val collectionNovelLazyListState = LazyListState()

    val trendingFilter = MutableStateFlow(Restrict.ALL)

    val illustsFollowing = Pager(PagingConfig(pageSize = 20)) {
        IllustFollowingPagingSource(restrict = trendingFilter.value)
    }.flow.cachedIn(viewModelScope)

    val novelsFollowing = Pager(PagingConfig(pageSize = 30)) {
        FollowNovelPagingSource(restrict = trendingFilter.value)
    }.flow.cachedIn(viewModelScope)

    val newNovels = Pager(PagingConfig(pageSize = 30)) {
        NovelNewPagingSource()
    }.flow.cachedIn(viewModelScope)

    val novelWatchlist = Pager(PagingConfig(pageSize = 30)) {
        NovelWatchlistPagingSource().also { activeNovelWatchlistSource = it }
    }.flow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            NovelWatchlistChanges.changes.collect {
                activeNovelWatchlistSource?.invalidate()
            }
        }
    }

    fun updateRestrict(restrict: Restrict) {
        trendingFilter.value = restrict
    }

    fun pagerStateFor(mode: AppViewMode): PagerState = when (mode) {
        AppViewMode.ILLUST -> illustPagerState
        AppViewMode.NOVEL -> novelPagerState
    }

    fun switchViewMode(mode: AppViewMode) {
        SettingRepository.setAppViewMode(mode)
    }
}
