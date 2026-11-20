package com.mrl.pixiv.common.analytics

expect val isUsageAnalyticsAvailable: Boolean

expect fun setUsageAnalyticsEnabled(enabled: Boolean)

/** Records fixed screen and page names for anonymous usage analytics. */
expect fun logScreenView(screen: String, page: String? = null)

expect fun logException(e: Throwable)
