package com.mrl.pixiv.common.util

import android.os.Build


actual object DeviceInfo {
    actual val PLATFORM = "Android"

    actual val VERSION: String
        get() = Build.VERSION.RELEASE

    actual val MODEL: String
        get() = Build.MODEL

    actual val APP_VERSION = "6.158.0"
    actual val DISPLAY_NAME by lazy {
        AppUtil.appContext.assets.open("device_mapping.csv").use {
            val reader = it.reader()
            val lines =
                reader.readLines().associate { it.split(",").let { it.first() to it.last() } }
            reader.close()

            lines[MODEL] ?: MODEL
        }
    }
}