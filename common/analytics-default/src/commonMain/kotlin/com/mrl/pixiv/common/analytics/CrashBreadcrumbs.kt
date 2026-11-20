package com.mrl.pixiv.common.analytics

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb

internal const val CrashBreadcrumbLimit = 100
internal const val AppCrashScreenCategory = "app.navigation"
internal const val AppCrashTabCategory = "app.navigation.tab"
internal const val AppCrashOperationCategory = "app.operation"

/** Records a compile-time screen name. Route arguments must stay out of diagnostics. */
fun recordCrashScreen(screen: String) = recordAppCrashBreadcrumb(AppCrashScreenCategory, screen)

/** Records a tab change using a fixed screen name and the enum name. */
fun trackTab(screen: String, tab: Enum<*>) {
    recordAppCrashBreadcrumb(AppCrashTabCategory, "$screen/${tab.name}")
    logScreenView(screen, tab.name)
}

/** Records a fixed app operation name. */
fun recordCrashOperation(operation: String) = recordAppCrashBreadcrumb(AppCrashOperationCategory, operation)

private fun recordAppCrashBreadcrumb(category: String, message: String) {
    if (Sentry.isEnabled()) {
        Sentry.addBreadcrumb(Breadcrumb().apply {
            this.category = category
            this.message = message
            level = SentryLevel.INFO
        })
    }
    addPlatformCrashBreadcrumb(category, message)
}

internal expect fun addPlatformCrashBreadcrumb(category: String, message: String)
