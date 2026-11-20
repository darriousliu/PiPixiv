package com.mrl.pixiv.common.analytics

import io.sentry.kotlin.multiplatform.Sentry

actual val isUsageAnalyticsAvailable: Boolean = false

actual fun setUsageAnalyticsEnabled(enabled: Boolean, clearLocalData: Boolean) = Unit

actual fun logEvent(
    event: String,
    params: Map<String, Any>?,
) {
}

actual fun logException(e: Throwable) {
    Sentry.captureException(e)
}
