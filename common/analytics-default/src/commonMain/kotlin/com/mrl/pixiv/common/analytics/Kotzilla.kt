package com.mrl.pixiv.common.analytics

import io.kotzilla.generated.monitoring
import org.koin.core.KoinApplication

fun KoinApplication.initKotzilla(isDebug: Boolean, versionName: String, displayName: String) {
    if (!isDebug) {
        monitoring {
            setProperties("deviceName" to displayName)
        }
    }
}