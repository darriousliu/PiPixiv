package com.mrl.pixiv.common.analytics

val isUsageAnalyticsAvailable: Boolean = false

fun setUsageAnalyticsEnabled(enabled: Boolean) = Unit

fun logScreenView(screen: String, page: String? = null) = Unit

fun logException(e: Throwable) {
    // todo foss lib
}
