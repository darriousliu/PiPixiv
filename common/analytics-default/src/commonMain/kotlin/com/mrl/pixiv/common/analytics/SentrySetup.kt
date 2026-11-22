package com.mrl.pixiv.common.analytics

import io.sentry.kotlin.multiplatform.Sentry

fun initializeSentry(
    isDebug: Boolean,
    dsn: String,
    appVersion: String,
    appBuild: Int,
    deviceName: () -> String,
) {
    if (isDebug || dsn.isBlank()) return

    initializePlatformSentry(dsn, appVersion, appBuild)
    if (!Sentry.isEnabled()) return

    val name = runCatching(deviceName).getOrNull()
    if (!name.isNullOrBlank()) {
        Sentry.configureScope { it.setTag("deviceName", name) }
    }
}

internal expect fun initializePlatformSentry(dsn: String, appVersion: String, appBuild: Int)
