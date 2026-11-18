package com.mrl.pixiv

import com.mrl.pixiv.common.repository.IosDownloadStrategy
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object IosBackgroundDownloads : KoinComponent {
    fun reconnectPending() {
        get<IosDownloadStrategy>().reconnectPendingDownloads()
    }

    fun handleEvents(identifier: String, onFinished: () -> Unit) {
        get<IosDownloadStrategy>().handleBackgroundEvents(identifier, onFinished)
    }
}
