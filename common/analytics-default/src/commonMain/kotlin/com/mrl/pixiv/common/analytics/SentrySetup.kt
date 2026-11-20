package com.mrl.pixiv.common.analytics

import io.sentry.kotlin.multiplatform.Sentry

fun initializeSentry(isDebug: Boolean, dsn: String, appVersion: String, appBuild: Int, deviceName: String) {
    if (isDebug || dsn.isBlank()) return

    initializePlatformSentry(dsn, appVersion, appBuild)
    Sentry.configureScope { it.setTag("deviceName", deviceName) }
}

internal expect fun initializePlatformSentry(dsn: String, appVersion: String, appBuild: Int)
