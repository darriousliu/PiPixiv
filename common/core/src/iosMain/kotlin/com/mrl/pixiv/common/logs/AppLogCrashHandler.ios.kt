package com.mrl.pixiv.common.logs

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.getUnhandledExceptionHook
import kotlin.native.setUnhandledExceptionHook
import kotlin.native.terminateWithUnhandledException

@OptIn(ExperimentalNativeApi::class)
internal actual fun chainPlatformAppLogCrashHandler() {
    val previous = getUnhandledExceptionHook()
    setUnhandledExceptionHook { error ->
        runCatching {
            AppLogStore.recordCrash(error, "AppCrash", "Unhandled exception")
        }
        if (previous != null) {
            previous(error)
        } else {
            terminateWithUnhandledException(error)
        }
    }
}
