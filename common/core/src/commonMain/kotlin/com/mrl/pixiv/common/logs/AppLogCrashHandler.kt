package com.mrl.pixiv.common.logs

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
private val crashHandlerInstalled = AtomicBoolean(false)

@OptIn(ExperimentalAtomicApi::class)
fun installAppLogCrashHandler() {
    if (crashHandlerInstalled.compareAndSet(expectedValue = false, newValue = true)) {
        chainPlatformAppLogCrashHandler()
    }
}

internal expect fun chainPlatformAppLogCrashHandler()
