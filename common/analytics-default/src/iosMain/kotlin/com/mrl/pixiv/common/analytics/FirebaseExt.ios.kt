package com.mrl.pixiv.common.analytics

import io.sentry.kotlin.multiplatform.Sentry

internal actual fun setPlatformUsageAnalyticsEnabled(enabled: Boolean) = Unit

internal actual fun logPlatformScreenView(screen: String, page: String?) = Unit

actual fun logException(e: Throwable) {
    Sentry.captureException(e)
}
