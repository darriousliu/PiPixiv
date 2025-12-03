package com.mrl.pixiv.common.datasource.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity

@Database(entities = [DownloadEntity::class], version = 1, exportSchema = false)
abstract class PixivDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
