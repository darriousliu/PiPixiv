package com.mrl.pixiv.common.util

import android.os.Build

actual object DeviceInfo {
    actual val PLATFORM = "Android"

    actual val VERSION: String
        get() = Build.VERSION.RELEASE

    actual val MODEL: String
        get() = Build.MODEL

    actual val APP_VERSION = "6.158.0"
}