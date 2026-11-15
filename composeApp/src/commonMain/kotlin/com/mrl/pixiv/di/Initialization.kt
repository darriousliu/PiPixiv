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
import com.mrl.pixiv.common.util.isAndroid
import com.mrl.pixiv.common.util.isDebug
import com.mrl.pixiv.common.util.platform
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
        // b/565042559: the bundled Android LinkComposer retains removed recomposition scopes.
        ComposeRuntimeFlags.isLinkBufferComposerEnabled = !platform.isAndroid()
    }
}

expect fun initializeMMKV(logLevel: MMKVLogLevel)
