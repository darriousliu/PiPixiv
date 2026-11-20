package com.mrl.pixiv

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composer
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import com.mrl.pixiv.common.analytics.FLAVOR
import com.mrl.pixiv.common.analytics.initializeFirebase
import com.mrl.pixiv.common.analytics.isUsageAnalyticsAvailable
import com.mrl.pixiv.common.analytics.setUsageAnalyticsEnabled
import com.mrl.pixiv.common.repository.AppRepository
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.isDebug
import com.mrl.pixiv.common.util.setAppCompatDelegateThemeMode
import com.mrl.pixiv.di.Initialization
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class App : Application() {
    companion object {
        lateinit var instance: App
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.Auto)
        instance = this
        initializeFirebase(isDebug)
        AppUtil.init(this, FLAVOR)
        Initialization.initKoin {
            androidLogger()
            androidContext(this@App)
        }
        if (isUsageAnalyticsAvailable) {
            setUsageAnalyticsEnabled(AppRepository.usageAnalyticsEnabled.value)
        }
        setAppCompatDelegateThemeMode(SettingRepository.settingTheme)
    }
}
