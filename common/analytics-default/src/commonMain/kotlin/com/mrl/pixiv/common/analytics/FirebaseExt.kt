package com.mrl.pixiv.common.analytics


expect fun logEvent(event: String, params: Map<String, Any>? = null)

expect fun logException(e: Throwable)