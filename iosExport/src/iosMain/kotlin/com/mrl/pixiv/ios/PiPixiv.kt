package com.mrl.pixiv.ios

import com.mrl.pixiv.IosBackgroundDownloads
import com.mrl.pixiv.MainViewController
import com.mrl.pixiv.common.util.PhotoUtil
import com.mrl.pixiv.common.util.ZipUtil
import com.mrl.pixiv.di.Initialization
import com.mrl.pixiv.di.initIOSKoin
import platform.UIKit.UIViewController

object PiPixiv {
    private var initialized = false

    fun initialize(
        zipUtil: ZipUtil,
        photoUtil: PhotoUtil,
    ) {
        if (initialized) return
        initialized = true
        Initialization.initKoin {
            initIOSKoin(
                di = listOf(zipUtil, photoUtil),
            )
        }
    }

    fun makeMainViewController(
        onStatusBarVisibilityChange: (Boolean) -> Unit,
    ): UIViewController = MainViewController(onStatusBarVisibilityChange)

    fun handleBackgroundDownloadEvents(identifier: String, onFinished: () -> Unit) {
        IosBackgroundDownloads.handleEvents(identifier, onFinished)
    }
}
