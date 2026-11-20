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

private val usageAnalyticsLock = Any()
private var usageAnalyticsEnabled = false
private val screenParameters = setOf(
    "screen_name",
    "screen_class",
    "page_name",
    "mode",
    "is_id_search",
    "search_mode",
    "enable_transition",
    "is_novel",
    "type",
)

actual val isUsageAnalyticsAvailable: Boolean = true

fun Application.initializeFirebase(isDebug: Boolean) {
    Firebase.initialize(this)
    Firebase.crashlytics.isCrashlyticsCollectionEnabled = !isDebug
}

actual fun setUsageAnalyticsEnabled(enabled: Boolean, clearLocalData: Boolean) {
    synchronized(usageAnalyticsLock) {
        usageAnalyticsEnabled = false
        val analytics = firebaseAnalytics
        if (!enabled || clearLocalData) {
            analytics.setAnalyticsCollectionEnabled(false)
        }
        val analyticsStorage = if (enabled) ConsentStatus.GRANTED else ConsentStatus.DENIED
        analytics.setConsent(
            mapOf(
                ConsentType.ANALYTICS_STORAGE to analyticsStorage,
                ConsentType.AD_STORAGE to ConsentStatus.DENIED,
                ConsentType.AD_USER_DATA to ConsentStatus.DENIED,
                ConsentType.AD_PERSONALIZATION to ConsentStatus.DENIED,
            ),
        )
        if (clearLocalData) analytics.resetAnalyticsData()
        if (enabled) {
            analytics.setAnalyticsCollectionEnabled(true)
            usageAnalyticsEnabled = true
        }
    }
}

actual fun logEvent(
    event: String,
    params: Map<String, Any>?,
) {
    synchronized(usageAnalyticsLock) {
        if (!usageAnalyticsEnabled || event != "screen_view" || params == null) return
        val screen = params["screen_name"] as? String ?: return
        if (!isDiagnosticIdentifier(screen)) return
        firebaseAnalytics.logEvent(event) {
            params.forEach { (key, value) ->
                if (key in screenParameters && value is String && isDiagnosticIdentifier(value)) {
                    param(key, value)
                }
            }
        }
    }
}
