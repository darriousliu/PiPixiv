package com.mrl.pixiv.common.analytics

import io.sentry.kotlin.multiplatform.Sentry

fun initializeSentry(isDebug: Boolean, dsn: String, displayName: String) {
    if (!isDebug) {
        Sentry.init { options ->
            options.dsn = dsn
            options.sendDefaultPii = false
        }
        Sentry.configureScope { scope ->
            scope.setTag("deviceName", displayName)
        }
    }
}
