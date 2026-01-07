package com.mrl.pixiv.common.analytics

import io.kotzilla.sdk.analytics.koin.analytics
import org.koin.core.KoinApplication

fun KoinApplication.initKotzilla(isDebug: Boolean, versionName: String, displayName: String) {
    if (!isDebug) {
        analytics {
            setApiKey("")
            setVersion(versionName)
            setProperties("deviceName" to displayName)
        }
    }
}