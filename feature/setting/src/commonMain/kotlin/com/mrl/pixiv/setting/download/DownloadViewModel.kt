package com.mrl.pixiv.setting.download

import androidx.lifecycle.viewModelScope
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.repository.DownloadManager
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel

data class DownloadState(
    val filterStatus: Int = FILTER_ALL
) {
    companion object {
        const val FILTER_ALL = -1
    }
}

@KoinViewModel
class DownloadViewModel(
    private val downloadManager: DownloadManager
) : BaseMviViewModel<DownloadState, ViewIntent>(DownloadState()) {
    val currentDownloads = uiState.map { it.filterStatus }
        .distinctUntilChanged()
        .flatMapLatest {
            if (it == DownloadState.FILTER_ALL) {
                downloadManager.getAllDownloads()
            } else {
                downloadManager.getDownloadsByStatus(it)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(2_000),
            initialValue = emptyList()
        )

    override suspend fun handleIntent(intent: ViewIntent) {
    }

    fun deleteDownload(entity: DownloadEntity) {
        launchIO {
            downloadManager.deleteDownload(entity)
        }
    }

    fun retryDownload(entity: DownloadEntity) {
        launchIO {
            downloadManager.retryDownload(entity)
        }
    }

    fun deleteAll() {
        launchIO {
            downloadManager.deleteAllDownloads()
        }
    }

    fun changeFilterStatus(status: Int) {
        updateState {
            copy(filterStatus = status)
        }
    }
}
