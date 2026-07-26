package com.mrl.pixiv.common.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NovelMarkerChanges {
    private val mutableChanges = MutableSharedFlow<Long>(
        extraBufferCapacity = 32,
    )

    val changes = mutableChanges.asSharedFlow()

    fun notifyChanged(novelId: Long) {
        mutableChanges.tryEmit(novelId)
    }
}
