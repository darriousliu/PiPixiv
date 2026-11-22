package com.mrl.pixiv.common.logs

import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter

internal actual fun chainPlatformAppLogCrashHandler() {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
        runCatching {
            AppLogStore.recordCrash(error, "AppCrash", "Unhandled exception")
        }
        if (previous != null) {
            previous.uncaughtException(thread, error)
        } else if (error !is ThreadDeath) {
            platformLogWriter().log(Severity.Assert, "Unhandled exception", "AppCrash", error)
        }
    }
}
