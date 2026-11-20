package com.mrl.pixiv.common.analytics

val isUsageAnalyticsAvailable: Boolean = false

fun setUsageAnalyticsEnabled(enabled: Boolean, clearLocalData: Boolean = true) = Unit

fun logEvent(event: String, params: Map<String, Any>? = null) {
    // todo foss lib
}

fun logException(e: Throwable) {
    // todo foss lib
}
