package com.mrl.pixiv.common.analytics

import Internal.Sentry.SentryCrashMonitorTypeCPPException
import Internal.Sentry.sentrycrash_setMonitoring
import Internal.Sentry.sentrycrashcm_getActiveMonitors
import io.sentry.kotlin.multiplatform.Sentry
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
    // Match Sentry KMP's C++ monitor option while keeping the Cocoa-specific privacy settings.
    // Restoring Kotlin/Native's terminate handler lets both Kotlin exception hooks run.
    val activeMonitors = sentrycrashcm_getActiveMonitors()
    if (activeMonitors and SentryCrashMonitorTypeCPPException != 0u) {
        sentrycrash_setMonitoring(activeMonitors and SentryCrashMonitorTypeCPPException.inv())
    }
}

internal actual fun addPlatformCrashBreadcrumb(category: String, message: String) = Unit
