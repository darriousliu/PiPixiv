package com.mrl.pixiv.di

import com.ctrip.flight.mmkv.initialize

actual fun initializeMMKV() {
    initialize()
}