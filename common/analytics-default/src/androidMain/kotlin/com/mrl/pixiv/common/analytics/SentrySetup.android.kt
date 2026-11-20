package com.mrl.pixiv.common.analytics

import io.sentry.kotlin.multiplatform.Sentry

internal actual fun initializePlatformSentry(dsn: String, appVersion: String, appBuild: Int) {
    Sentry.initWithPlatformOptions { options ->
        options.configureCrashReports(dsn)
        options.isEnableActivityLifecycleBreadcrumbs = true
        options.isEnableAppLifecycleBreadcrumbs = true
        options.isEnableSystemEventBreadcrumbs = true
        options.isEnableAppComponentBreadcrumbs = true
        options.isEnableNetworkEventBreadcrumbs = true
        options.isEnableAutoActivityLifecycleTracing = false
        options.isEnableScopeSync = true
        options.isAttachAnrThreadDump = true
        options.isAttachScreenshot = false
        options.isAttachViewHierarchy = false
        options.sessionReplay.sessionSampleRate = 0.0
        options.sessionReplay.onErrorSampleRate = 0.0
    }
}
