package com.mrl.pixiv.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.ctrip.flight.mmkv.MMKVLogLevel
import com.mrl.pixiv.common.analytics.initKotzilla
import com.mrl.pixiv.common.analytics.initializeSentry
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.DeviceInfo
import com.mrl.pixiv.common.util.isDebug
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

object Initialization {
    fun initKoin(platformKoinAppDeclaration: KoinAppDeclaration = {}) {
        Logger.setMinSeverity(if (isDebug) Severity.Debug else Severity.Error)
        initializeSentry(isDebug, AppUtil.sentryDsn)
        initializeMMKV(logLevel = MMKVLogLevel.LevelInfo)
        startKoin {
            platformKoinAppDeclaration()
            initKotzilla(isDebug, AppUtil.versionName, DeviceInfo.DISPLAY_NAME)
            modules(allModule)
        }
        BlockingRepositoryV2.migrate()
    }
}

expect fun initializeMMKV(logLevel: MMKVLogLevel)