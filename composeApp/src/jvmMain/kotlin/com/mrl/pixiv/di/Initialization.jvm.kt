package com.mrl.pixiv.di

import com.ctrip.flight.mmkv.MMKVLogLevel
import com.ctrip.flight.mmkv.initialize
import com.mrl.pixiv.common.analytics.logException
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir
import kotlin.system.exitProcess

actual fun initializeMMKV(logLevel: MMKVLogLevel) {
    try {
        initialize((FileKit.filesDir / "mmkv").absolutePath(), logLevel)
    } catch (e: Exception) {
        logException(e)
        exitProcess(-1)
    }
}