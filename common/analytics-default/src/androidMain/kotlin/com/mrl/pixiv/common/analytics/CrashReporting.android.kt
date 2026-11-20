package com.mrl.pixiv.common.analytics

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import io.sentry.kotlin.multiplatform.Sentry

internal actual fun addPlatformCrashBreadcrumb(category: String, message: String) {
    addJavaCrashBreadcrumb(category, message)
    val crashlytics = Firebase.crashlytics
    if (!crashlytics.isCrashlyticsCollectionEnabled) return
    crashlytics.log("[$category] $message")
    if (category == AppCrashScreenCategory) crashlytics.setCustomKey("current_screen", message)
}

actual fun logException(e: Throwable) {
    Sentry.captureException(e)
    val crashlytics = Firebase.crashlytics
    if (crashlytics.isCrashlyticsCollectionEnabled) {
        crashlytics.recordException(e)
    }
}
