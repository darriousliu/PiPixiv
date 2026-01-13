package com.mrl.pixiv.common.analytics

import io.sentry.kotlin.multiplatform.Sentry

fun initializeSentry(isDebug: Boolean, dsn: String) {
    if (isDebug) {
        Sentry.init { options ->
            options.dsn = dsn
            options.sendDefaultPii = false
        }
    }
}