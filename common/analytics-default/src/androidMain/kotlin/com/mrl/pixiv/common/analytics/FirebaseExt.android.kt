package com.mrl.pixiv.common.analytics

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.initialize

private val firebaseAnalytics
    get() = Firebase.analytics

private val firebaseCrashlytics
    get() = Firebase.crashlytics

fun Application.initializeFirebase(isDebug: Boolean) {
    Firebase.initialize(this)
    Firebase.crashlytics.isCrashlyticsCollectionEnabled = !isDebug
}

actual fun logEvent(
    event: String,
    params: Map<String, Any>?
) {
    firebaseAnalytics.logEvent(event) {
        params?.forEach { (k, v) ->
            when (v) {
                is String -> param(k, v)
                is Int -> param(k, v.toLong())
                is Long -> param(k, v)
                is Float -> param(k, v.toDouble())
                is Double -> param(k, v)
                is Boolean -> param(k, v.toString())
                else -> param(k, v.toString())
            }
        }
    }
}

actual fun logException(e: Throwable) {
    firebaseCrashlytics.recordException(e)
}