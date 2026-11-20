package com.mrl.pixiv.common.analytics

import io.sentry.kotlin.multiplatform.Sentry

actual val isUsageAnalyticsAvailable: Boolean = false

actual fun setUsageAnalyticsEnabled(enabled: Boolean) = Unit

actual fun logScreenView(screen: String, page: String?) = Unit

actual fun logException(e: Throwable) {
    Sentry.captureException(e)
}
