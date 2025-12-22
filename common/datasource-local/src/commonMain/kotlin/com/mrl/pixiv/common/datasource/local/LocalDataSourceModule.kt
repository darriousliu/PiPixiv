package com.mrl.pixiv.common.datasource.local

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.annotation.Single

@Single
fun provideDatabase(builder: RoomDatabase.Builder<PixivDatabase>): PixivDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(PixivDatabase.MIGRATION_1_2, PixivDatabase.MIGRATION_2_3)
        .build()
}

@Single
fun provideDownloadDao(database: PixivDatabase): DownloadDao = database.downloadDao()
