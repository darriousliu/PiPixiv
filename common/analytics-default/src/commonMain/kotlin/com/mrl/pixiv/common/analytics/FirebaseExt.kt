package com.mrl.pixiv.common.analytics

import io.sentry.kotlin.multiplatform.Sentry
import kotlin.concurrent.Volatile

const val isUsageAnalyticsAvailable: Boolean = true

@Volatile
private var usageAnalyticsEnabled = false

fun setUsageAnalyticsEnabled(enabled: Boolean) {
    usageAnalyticsEnabled = enabled
    setPlatformUsageAnalyticsEnabled(enabled)
}

/** Records fixed screen and page names for anonymous usage analytics. */
fun logScreenView(screen: String, page: String? = null) {
    if (!usageAnalyticsEnabled) return

    if (Sentry.isEnabled()) {
        Sentry.logger.info("screen_view") {
            this["screen_name"] = screen
            if (page != null) this["page_name"] = page
        }
    }
    logPlatformScreenView(screen, page)
}

internal expect fun setPlatformUsageAnalyticsEnabled(enabled: Boolean)

internal expect fun logPlatformScreenView(screen: String, page: String?)

expect fun logException(e: Throwable)
