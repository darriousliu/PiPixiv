package com.mrl.pixiv.common.util

import io.kotzilla.sdk.analytics.koin.analytics
import org.koin.core.KoinApplication

fun KoinApplication.initKotzilla() {
    if (!isDebug) {
        analytics()
    }
}