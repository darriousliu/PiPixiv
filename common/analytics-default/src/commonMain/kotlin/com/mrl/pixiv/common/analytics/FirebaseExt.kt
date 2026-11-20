package com.mrl.pixiv.common.analytics

expect val isUsageAnalyticsAvailable: Boolean

expect fun setUsageAnalyticsEnabled(enabled: Boolean, clearLocalData: Boolean = true)

expect fun logEvent(event: String, params: Map<String, Any>? = null)

expect fun logException(e: Throwable)
