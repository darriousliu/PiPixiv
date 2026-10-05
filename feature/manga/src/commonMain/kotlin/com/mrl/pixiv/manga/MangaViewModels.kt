package com.mrl.pixiv.manga

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.manga.MangaSeriesContextResp
import com.mrl.pixiv.common.data.manga.MangaSeriesDetail
import com.mrl.pixiv.common.repository.ReadingRepository
import com.mrl.pixiv.common.repository.paging.MangaSeriesPagingSource
import com.mrl.pixiv.common.repository.paging.MangaWatchlistPagingSource
import com.mrl.pixiv.common.repository.paging.UserMangaSeriesPagingSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class MangaSeriesViewModel(val seriesId: Long) : ViewModel() {
    private val _detail = MutableStateFlow<MangaSeriesDetail?>(null)
    val detail = _detail.asStateFlow()
    private val _updating = MutableStateFlow(false)
    val updating = _updating.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private var watchlistVersion = 0L
    private var lastWatchlistChange: Boolean? = null

    val works = Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
        val requestVersion = watchlistVersion
        MangaSeriesPagingSource(seriesId, onDetail = { response ->
            // An older in-flight refresh must not undo a completed follow/unfollow.
            val added = lastWatchlistChange
            _detail.value = if (requestVersion != watchlistVersion && added != null) {
                response.copy(watchlistAdded = added)
            } else response
        })
    }.flow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            ReadingRepository.watchlistChanges.collect { (id, added) ->
                if (id == seriesId) {
                    watchlistVersion++
                    lastWatchlistChange = added
                    _detail.update { it?.copy(watchlistAdded = added) }
                }
            }
        }
    }

    fun toggleWatchlist() {
        val current = detail.value ?: return
        if (_updating.value) return
        _updating.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                ReadingRepository.setMangaWatchlist(seriesId, !current.watchlistAdded)
                _detail.update { it?.copy(watchlistAdded = !current.watchlistAdded) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _error.value = error.message.orEmpty()
            } finally {
                _updating.value = false
            }
        }
    }
}

@KoinViewModel
class MangaWatchlistViewModel : ViewModel() {
    private var activeSource: MangaWatchlistPagingSource? = null
    val series = Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
        MangaWatchlistPagingSource().also { activeSource = it }
    }.flow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            ReadingRepository.watchlistChanges.collect { activeSource?.invalidate() }
        }
    }
}

@KoinViewModel
class UserMangaSeriesViewModel(val userId: Long) : ViewModel() {
    val series = Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
        UserMangaSeriesPagingSource(userId)
    }.flow.cachedIn(viewModelScope)
}

@KoinViewModel
class MangaContextViewModel(val illustId: Long) : ViewModel() {
    private val _state = MutableStateFlow(MangaContextState())
    val state = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            ReadingRepository.watchlistChanges.collect { (id, added) ->
                _state.update { current ->
                    val response = current.response
                    val detail = response?.detail
                    if (response != null && detail?.id == id) {
                        current.copy(response = response.copy(detail = detail.copy(watchlistAdded = added)))
                    } else current
                }
            }
        }
    }

    fun load() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val response = ReadingRepository.mangaContext(illustId)
                _state.update { it.copy(response = response, loading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.message.orEmpty()) }
            }
        }
    }
}

data class MangaContextState(
    val response: MangaSeriesContextResp? = null,
    val loading: Boolean = false,
    val error: String? = null,
)
