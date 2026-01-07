package com.mrl.pixiv.common.util

import org.jetbrains.skiko.OS

actual object DeviceInfo {
    actual val PLATFORM = "android"
    actual val VERSION = "15"
    actual val MODEL = "Pixel 9 Pro XL"
    actual val APP_VERSION = "6.158.0"
    actual val DISPLAY_NAME by lazy {
        val os = System.getProperty("os.name")
        return@lazy when {
            os.equals("Mac OS X", ignoreCase = true) -> OS.MacOS
            os.startsWith("Win", ignoreCase = true) -> OS.Windows
            os.startsWith("Linux", ignoreCase = true) -> OS.Linux
            else -> error("Unknown OS name: $os")
        }.name
    }
}