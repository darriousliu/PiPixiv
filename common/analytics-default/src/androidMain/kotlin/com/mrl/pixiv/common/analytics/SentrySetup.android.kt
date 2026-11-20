package com.mrl.pixiv.common.analytics

import io.sentry.kotlin.multiplatform.Sentry

internal actual fun initializePlatformSentry(dsn: String, appVersion: String, appBuild: Int) {
    Sentry.initWithPlatformOptions { options ->
        options.configureCrashReports(dsn)
        options.setEnableActivityLifecycleBreadcrumbs(true)
        options.setEnableAppLifecycleBreadcrumbs(true)
        options.setEnableSystemEventBreadcrumbs(true)
        options.setEnableAppComponentBreadcrumbs(true)
        options.setEnableNetworkEventBreadcrumbs(true)
        options.setEnableAutoActivityLifecycleTracing(false)
        options.setEnableScopeSync(true)
        options.setAttachAnrThreadDump(true)
        options.setAttachScreenshot(false)
        options.setAttachViewHierarchy(false)
        options.sessionReplay.sessionSampleRate = 0.0
        options.sessionReplay.onErrorSampleRate = 0.0
    }
}
