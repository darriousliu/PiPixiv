package com.mrl.pixiv.common.analytics

fun initializeSentry(isDebug: Boolean, dsn: String, appVersion: String, appBuild: Int) {
    if (isDebug || dsn.isBlank()) return

    initializePlatformSentry(dsn, appVersion, appBuild)
}

internal expect fun initializePlatformSentry(dsn: String, appVersion: String, appBuild: Int)
