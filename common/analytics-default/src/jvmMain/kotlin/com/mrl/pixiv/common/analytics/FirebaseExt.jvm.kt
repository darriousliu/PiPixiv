package com.mrl.pixiv.common.analytics

actual fun logEvent(
    event: String,
    params: Map<String, Any>?
) {
}

actual fun logException(e: Throwable) {
}