package com.mrl.pixiv.di

import com.ctrip.flight.mmkv.MMKVLogLevel
import com.ctrip.flight.mmkv.initialize

actual fun initializeMMKV(logLevel: MMKVLogLevel) {
    initialize(logLevel = logLevel)
}