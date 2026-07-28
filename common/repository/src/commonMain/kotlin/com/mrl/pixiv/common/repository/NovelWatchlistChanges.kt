package com.mrl.pixiv.common.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NovelWatchlistChanges {
    private val mutableChanges = MutableSharedFlow<Long>(
        extraBufferCapacity = 32,
    )

    val changes = mutableChanges.asSharedFlow()

    fun notifyChanged(seriesId: Long) {
        mutableChanges.tryEmit(seriesId)
    }
}
