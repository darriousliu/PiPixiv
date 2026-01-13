package com.mrl.pixiv.common.analytics

import io.sentry.kotlin.multiplatform.Sentry

actual fun logEvent(
    event: String,
    params: Map<String, Any>?
) {
}

actual fun logException(e: Throwable) {
    Sentry.captureException(e)
}