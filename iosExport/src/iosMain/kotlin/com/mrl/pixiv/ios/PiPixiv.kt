package com.mrl.pixiv.ios

import com.mrl.pixiv.MainViewController
import com.mrl.pixiv.common.util.PhotoUtil
import com.mrl.pixiv.common.util.ZipUtil
import com.mrl.pixiv.di.Initialization
import com.mrl.pixiv.di.initIOSKoin
import platform.UIKit.UIViewController

object PiPixiv {
    fun initialize(
        zipUtil: ZipUtil,
        photoUtil: PhotoUtil,
    ) {
        Initialization.initKoin {
            initIOSKoin(
                di = listOf(zipUtil, photoUtil),
            )
        }
    }

    fun makeMainViewController(): UIViewController = MainViewController()
}
