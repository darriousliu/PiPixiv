package com.mrl.pixiv.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.UserManager
import com.mrl.pixiv.common.repository.paging.IllustRecommendedPagingSource
import com.mrl.pixiv.common.repository.paging.MangaRecommendedPagingSource
import com.mrl.pixiv.common.repository.paging.NovelRecommendedPagingSource
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.common.viewmodel.ViewIntent
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.KoinComponent

@Stable
data object HomeState

sealed class HomeAction : ViewIntent

sealed class HomeSideEffect : SideEffect {
    data class RefreshImage(val mode: HomeImageFeedMode) : HomeSideEffect()
    data object RefreshNovel : HomeSideEffect()
}

enum class HomeImageFeedMode {
    Illust,
    Manga
}

@KoinViewModel
class HomeViewModel : BaseMviViewModel<HomeState, HomeAction>(
    initialState = HomeState
), KoinComponent {
    val lazyStaggeredGridState by lazy { LazyStaggeredGridState() }
    val mangaLazyStaggeredGridState by lazy { LazyStaggeredGridState() }
    var imageFeedMode by mutableStateOf(HomeImageFeedMode.Illust)
        private set

    val recommendImageList by lazy {
        Pager(PagingConfig(pageSize = 20)) {
            IllustRecommendedPagingSource()
        }.flow.cachedIn(viewModelScope)
    }

    val recommendMangaList by lazy {
        Pager(PagingConfig(pageSize = 20)) {
            MangaRecommendedPagingSource()
        }.flow.cachedIn(viewModelScope)
    }

    val lazyListState by lazy { LazyListState() }
    val recommendNovelList by lazy {
        Pager(PagingConfig(pageSize = 30)) {
            NovelRecommendedPagingSource()
        }.flow.cachedIn(viewModelScope)
    }

    init {
        loadUserInfo()
    }

    override suspend fun handleIntent(intent: HomeAction) {
    }

    private fun loadUserInfo() {
        launchIO {
            UserManager.updateUserInfoAsync()
        }
    }

    fun refresh(mode: AppViewMode) {
        sendEffect(
            when (mode) {
                AppViewMode.ILLUST -> HomeSideEffect.RefreshImage(imageFeedMode)
                AppViewMode.NOVEL -> HomeSideEffect.RefreshNovel
            }
        )
    }

    fun switchViewMode(mode: AppViewMode) {
        SettingRepository.setAppViewMode(mode)
    }

    fun switchImageFeedMode(mode: HomeImageFeedMode) {
        imageFeedMode = mode
    }
}
