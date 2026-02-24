package com.mrl.pixiv.common.util

import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIUserInterfaceIdiomPhone

actual val platform: Platform by lazy {
    val name = UIDevice.currentDevice.userInterfaceIdiom
    when (name) {
        UIUserInterfaceIdiomPhone -> Platform.Apple.IPhoneOS
        UIUserInterfaceIdiomPad -> Platform.Apple.IPadOS
        else -> error("unknown platform")
    }
}