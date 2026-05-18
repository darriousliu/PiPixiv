package com.mrl.pixiv.common.datasource.local

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mrl.pixiv.common.datasource.local.dao.BlockContentDao
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.dao.NovelReadingProgressDao
import com.mrl.pixiv.common.datasource.local.dao.NovelTranslationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.annotation.Single

@Single
fun provideDatabase(builder: RoomDatabase.Builder<PixivDatabase>): PixivDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(
            PixivDatabase.MIGRATION_1_2,
            PixivDatabase.MIGRATION_2_3,
            PixivDatabase.MIGRATION_3_4,
            PixivDatabase.MIGRATION_4_5,
            PixivDatabase.MIGRATION_5_6
        )
        .build()
}

@Single
fun provideBlockContentDao(database: PixivDatabase): BlockContentDao = database.blockContentDao()

@Single
fun provideDownloadDao(database: PixivDatabase): DownloadDao = database.downloadDao()

@Single
fun provideNovelReadingProgressDao(database: PixivDatabase): NovelReadingProgressDao =
    database.novelReadingProgressDao()

@Single
fun provideNovelTranslationDao(database: PixivDatabase): NovelTranslationDao =
    database.novelTranslationDao()
