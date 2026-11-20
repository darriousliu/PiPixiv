package com.mrl.pixiv.di

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import com.ctrip.flight.mmkv.MMKVLogLevel
import com.mrl.pixiv.common.analytics.initializeSentry
import com.mrl.pixiv.common.analytics.isUsageAnalyticsAvailable
import com.mrl.pixiv.common.analytics.setUsageAnalyticsEnabled
import com.mrl.pixiv.common.logs.AppLogStore
import com.mrl.pixiv.common.logs.installAppLogCrashHandler
import com.mrl.pixiv.common.repository.AppRepository
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.DeviceInfo
import com.mrl.pixiv.common.util.isDebug
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

object Initialization {
    fun initKoin(platformKoinAppDeclaration: KoinAppDeclaration = {}) {
        AppLogStore.start()
        val systemWriter = platformLogWriter()
        Logger.setLogWriters(if (isDebug) systemWriter else ErrorLogWriter(systemWriter), AppLogStore.writer)
        Logger.setMinSeverity(if (isDebug) Severity.Debug else Severity.Info)
        try {
            initializeSentry(
                isDebug = isDebug,
                dsn = AppUtil.sentryDsn,
                appVersion = AppUtil.versionName,
                appBuild = AppUtil.versionCode,
                deviceName = { DeviceInfo.DISPLAY_NAME },
            )
        } catch (error: Throwable) {
            AppLogStore.recordCrash(error, "Sentry", "Crash reporter initialization failed")
            throw error
        }
        installAppLogCrashHandler()
        initializeMMKV(logLevel = MMKVLogLevel.LevelInfo)
        if (isUsageAnalyticsAvailable) {
            setUsageAnalyticsEnabled(AppRepository.usageAnalyticsEnabled.value)
        }
        startKoin {
            platformKoinAppDeclaration()
            modules(allModule)
        }
        BlockingRepositoryV2.migrate()
    }
}

private class ErrorLogWriter(private val delegate: LogWriter) : LogWriter() {
    override fun isLoggable(tag: String, severity: Severity): Boolean =
        severity >= Severity.Error && delegate.isLoggable(tag, severity)

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) =
        delegate.log(severity, message, tag, throwable)
}

expect fun initializeMMKV(logLevel: MMKVLogLevel)
