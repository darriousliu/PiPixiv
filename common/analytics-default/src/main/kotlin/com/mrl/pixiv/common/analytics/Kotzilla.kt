package com.mrl.pixiv.common.analytics

import io.kotzilla.sdk.analytics.koin.analytics
import org.koin.core.KoinApplication

fun KoinApplication.initKotzilla(isDebug: Boolean) {
    if (!isDebug) {
        analytics()
    }
}