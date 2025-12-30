package com.mrl.pixiv

import android.app.Application
import android.content.Context
import com.mrl.pixiv.common.analytics.FLAVOR
import com.mrl.pixiv.common.analytics.initializeFirebase
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.VersionManager
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.deleteFiles
import com.mrl.pixiv.common.util.isDebug
import com.mrl.pixiv.common.util.isExist
import com.mrl.pixiv.common.util.setAppCompatDelegateThemeMode
import com.mrl.pixiv.di.Initialization
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

@OptIn(DelicateCoroutinesApi::class)
class App : Application() {
    companion object {
        lateinit var instance: App
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeFirebase(isDebug)
        AppUtil.init(this, FLAVOR)
        Initialization.initKoin {
            androidLogger()
            androidContext(this@App)
        }
        migrateDataStoreToMMKV()
        migrateBlockingToNewFile()
        setAppCompatDelegateThemeMode(SettingRepository.settingTheme)
        VersionManager.checkUpdate()
    }

    private fun migrateDataStoreToMMKV() {
        val mmkv = MMKV.mmkvWithID("pixiv.user")
        runBlocking(Dispatchers.IO) {
            if (filesDir.resolve("mmkv").exists()) {
                return@runBlocking
            }
            val search = async {
                val file = filesDir.resolve("datastore/search.pb")
                if (file.isExist) {
                    val bytes = file.readBytes()
                    mmkv.encode("searchHistory", bytes)
                    deleteFiles(file)
                }
            }
            val userPreference = async {
                val file = filesDir.resolve("datastore/user_preference.pb")
                if (file.isExist) {
                    val bytes = file.readBytes()
                    mmkv.encode("userPreference", bytes)
                    deleteFiles(file)
                }
            }
            val userInfo = async {
                val file = filesDir.resolve("datastore/user_info.pb")
                if (file.isExist) {
                    val bytes = file.readBytes()
                    mmkv.encode("userInfo", bytes)
                    deleteFiles(file)
                }
            }
            awaitAll(search, userPreference, userInfo)
        }
    }

    private fun migrateBlockingToNewFile() {
        BlockingRepositoryV2.migrate()
    }
}
