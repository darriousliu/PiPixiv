package com.mrl.pixiv.common.datasource.local

import android.content.Context
import androidx.room.Room
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import org.koin.core.annotation.Single

@Single
fun provideDatabase(context: Context): PixivDatabase {
    return Room.databaseBuilder(
        context,
        PixivDatabase::class.java,
        "pixiv_db"
    ).build()
}

@Single
fun provideDownloadDao(database: PixivDatabase): DownloadDao = database.downloadDao()
