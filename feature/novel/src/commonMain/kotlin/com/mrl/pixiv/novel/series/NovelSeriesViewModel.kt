package com.mrl.pixiv.novel.series

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.novel.NovelSeriesDetail
import com.mrl.pixiv.common.repository.NovelWatchlistChanges
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.paging.NovelSeriesPagingSource
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.strings.load_failed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@Stable
data class NovelSeriesState(
    val detail: NovelSeriesDetail? = null,
    val isUpdatingWatchlist: Boolean = false,
) {
    fun withWatchlistAdded(isAdded: Boolean): NovelSeriesState = copy(
        detail = detail?.copy(watchlistAdded = isAdded),
    )
}

@KoinViewModel
class NovelSeriesViewModel(
    val seriesId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(NovelSeriesState())
    val state = _state.asStateFlow()

    val novels = Pager(
        config = PagingConfig(
            pageSize = 30,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = {
            NovelSeriesPagingSource(
                seriesId = seriesId,
                onSeriesDetail = { detail ->
                    _state.update { it.copy(detail = detail) }
                },
            )
        },
    ).flow.cachedIn(viewModelScope)

    fun toggleWatchlist() {
        val detail = state.value.detail ?: return
        if (state.value.isUpdatingWatchlist) return

        viewModelScope.launch {
            _state.update { it.copy(isUpdatingWatchlist = true) }
            try {
                if (detail.watchlistAdded) {
                    PixivRepository.deleteNovelSeriesFromWatchlist(seriesId)
                } else {
                    PixivRepository.addNovelSeriesToWatchlist(seriesId)
                }
                _state.update { current ->
                    current.withWatchlistAdded(!detail.watchlistAdded)
                }
                NovelWatchlistChanges.notifyChanged(seriesId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                ToastUtil.safeShortToast(RStrings.load_failed, error.message.orEmpty())
            } finally {
                _state.update { it.copy(isUpdatingWatchlist = false) }
            }
        }
    }
}
