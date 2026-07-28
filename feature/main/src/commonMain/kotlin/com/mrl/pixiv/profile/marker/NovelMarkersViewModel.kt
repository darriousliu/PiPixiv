package com.mrl.pixiv.profile.marker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.repository.NovelMarkerChanges
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.paging.NovelMarkerPagingSource
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.strings.novel_marker_delete_success
import com.mrl.pixiv.strings.novel_marker_update_failed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class NovelMarkersViewModel : ViewModel() {
    private var activePagingSource: NovelMarkerPagingSource? = null
    private val deletingNovelIds = mutableSetOf<Long>()

    val markers = Pager(
        config = PagingConfig(pageSize = 30),
    ) {
        NovelMarkerPagingSource().also { activePagingSource = it }
    }.flow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            NovelMarkerChanges.changes.collect {
                activePagingSource?.invalidate()
            }
        }
    }

    fun deleteMarker(novelId: Long) {
        if (!deletingNovelIds.add(novelId)) return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    PixivRepository.postNovelMarkerDelete(novelId)
                }
                NovelMarkerChanges.notifyChanged(novelId)
                ToastUtil.safeShortToast(RStrings.novel_marker_delete_success)
            } catch (throwable: CancellationException) {
                throw throwable
            } catch (throwable: Exception) {
                ToastUtil.safeShortToast(
                    RStrings.novel_marker_update_failed,
                    throwable.message.orEmpty(),
                )
            } finally {
                deletingNovelIds.remove(novelId)
            }
        }
    }
}
