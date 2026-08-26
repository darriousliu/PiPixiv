package com.mrl.pixiv.di

import androidx.compose.runtime.ComposeRuntimeFlags
import androidx.compose.runtime.ExperimentalComposeApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.ctrip.flight.mmkv.MMKVLogLevel
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
        initializeSentry(isDebug, AppUtil.sentryDsn, DeviceInfo.DISPLAY_NAME)
        initializeMMKV(logLevel = MMKVLogLevel.LevelInfo)
        startKoin {
            platformKoinAppDeclaration()
            modules(allModule)
        }
        BlockingRepositoryV2.migrate()
        initComposeRuntimeFlags()
    }

    @OptIn(ExperimentalComposeApi::class)
    fun initComposeRuntimeFlags() {
        ComposeRuntimeFlags.isLinkBufferComposerEnabled = true
    }
}

expect fun initializeMMKV(logLevel: MMKVLogLevel)
