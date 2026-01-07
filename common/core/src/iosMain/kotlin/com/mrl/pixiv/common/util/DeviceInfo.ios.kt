package com.mrl.pixiv.common.util

import platform.UIKit.UIDevice

actual object DeviceInfo {
    private val systemName = UIDevice.currentDevice.systemName
    actual val PLATFORM = systemName

    actual val VERSION: String
        get() = UIDevice.currentDevice.systemVersion

    actual val MODEL: String
        get() = UIDevice.currentDevice.model

    actual val APP_VERSION = "8.4.0"
    actual val DISPLAY_NAME by lazy {
        UIDevice.currentDevice.model
    }
}