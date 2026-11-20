package com.mrl.pixiv.common.analytics

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics.ConsentStatus
import com.google.firebase.analytics.FirebaseAnalytics.ConsentType
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.initialize

private val firebaseAnalytics
    get() = Firebase.analytics

actual val isUsageAnalyticsAvailable: Boolean = true

fun Application.initializeFirebase(isDebug: Boolean) {
    Firebase.initialize(this)
    Firebase.crashlytics.isCrashlyticsCollectionEnabled = !isDebug
}

actual fun setUsageAnalyticsEnabled(enabled: Boolean) {
    val analytics = firebaseAnalytics
    val analyticsStorage = if (enabled) ConsentStatus.GRANTED else ConsentStatus.DENIED
    analytics.setConsent(mapOf(ConsentType.ANALYTICS_STORAGE to analyticsStorage))
    analytics.setAnalyticsCollectionEnabled(enabled)
    if (!enabled) analytics.resetAnalyticsData()
}

actual fun logScreenView(screen: String, page: String?) {
    firebaseAnalytics.logEvent("screen_view") {
        param("screen_name", screen)
        if (page != null) param("page_name", page)
    }
}
