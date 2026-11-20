package com.mrl.pixiv.common.analytics

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal actual fun initializePlatformSentry(dsn: String, appVersion: String, appBuild: Int) {
    Sentry.initWithPlatformOptions { options ->
        options.setDsn(dsn)
        options.setSendDefaultPii(false)
        options.setEnableAutoSessionTracking(true)
        options.setEnableAutoBreadcrumbTracking(true)
        options.setEnableNetworkBreadcrumbs(true)
        options.setMaxBreadcrumbs(CrashBreadcrumbLimit.toULong())
        options.setBeforeBreadcrumb { breadcrumb ->
            breadcrumb?.apply {
                stripHttpBreadcrumbQueryAndFragment(category, type, data) { key, value ->
                    setDataValue(value, forKey = key)
                }
            }
        }
        options.setTracesSampleRate(null)
        options.setTracePropagationTargets(emptyList<Any>())
        options.setEnableAutoPerformanceTracing(false)
        options.setEnableNetworkTracking(false)
        options.setEnableLogs(false)
        options.sessionReplay().apply {
            setSessionSampleRate(0f)
            setOnErrorSampleRate(0f)
        }
        options.setAttachScreenshot(false)
        options.setAttachViewHierarchy(false)
    }
}

internal actual fun addPlatformCrashBreadcrumb(category: String, message: String) {
    if (!Sentry.isEnabled()) return
    Sentry.addBreadcrumb(Breadcrumb().apply {
        this.category = category
        this.message = message
        level = SentryLevel.INFO
    })
}
