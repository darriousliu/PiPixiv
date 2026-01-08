package com.mrl.pixiv.di

import com.ctrip.flight.mmkv.MMKVLogLevel
import com.mrl.pixiv.common.analytics.initKotzilla
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.DeviceInfo
import com.mrl.pixiv.common.util.isDebug
import com.mrl.pixiv.common.util.isDesktop
import com.mrl.pixiv.common.util.platform
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

object Initialization {
    fun initKoin(platformKoinAppDeclaration: KoinAppDeclaration = {}) {
        initializeMMKV(logLevel = MMKVLogLevel.LevelInfo)
        startKoin {
            platformKoinAppDeclaration()
            if (!platform.isDesktop()) {
                initKotzilla(isDebug, AppUtil.versionName, DeviceInfo.DISPLAY_NAME)
            }
            modules(allModule)
        }
    }
}

expect fun initializeMMKV(logLevel: MMKVLogLevel)