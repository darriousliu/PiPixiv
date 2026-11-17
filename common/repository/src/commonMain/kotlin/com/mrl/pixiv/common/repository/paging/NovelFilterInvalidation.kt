package com.mrl.pixiv.common.repository.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.mrl.pixiv.common.repository.NovelFilterSettingsChanges
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

fun <Key : Any, Value : Any> novelFilterPagingFlow(
    config: PagingConfig,
    pagingSourceFactory: () -> PagingSource<Key, Value>,
): Flow<PagingData<Value>> = flow {
    val activeSource = MutableStateFlow<PagingSource<Key, Value>?>(null)
    val unregister = NovelFilterSettingsChanges.register {
        activeSource.value?.invalidate()
    }
    try {
        emitAll(Pager(config) {
            pagingSourceFactory().also { activeSource.value = it }
        }.flow)
    } finally {
        // Paging 取消收集时可能不会使最后一代源失效。
        unregister()
    }
}
