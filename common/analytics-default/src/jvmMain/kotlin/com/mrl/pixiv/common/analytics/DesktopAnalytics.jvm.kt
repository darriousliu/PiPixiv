package com.mrl.pixiv.common.analytics

import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.exception.ExceptionMechanismException
import io.sentry.protocol.Mechanism

fun captureDesktopFatalException(error: Throwable) {
    val mechanism = Mechanism().apply {
        type = "TaoFatalError"
        isHandled = false
    }
    val event = SentryEvent(ExceptionMechanismException(mechanism, error, Thread.currentThread()))
        .apply { level = SentryLevel.FATAL }
    // The desktop SDK applies the same app and runtime context as for non-fatal errors.
    io.sentry.Sentry.captureEvent(event)
}

fun flushDesktopAnalytics(timeoutMillis: Long) {
    io.sentry.Sentry.flush(timeoutMillis)
}
