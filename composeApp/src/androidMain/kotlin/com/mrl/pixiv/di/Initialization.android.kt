package com.mrl.pixiv.di

import com.ctrip.flight.mmkv.MMKVLogLevel
import com.ctrip.flight.mmkv.initialize
import com.mrl.pixiv.common.util.AppUtil

actual fun initializeMMKV(logLevel: MMKVLogLevel) {
    initialize(AppUtil.appContext, logLevel)
}