package com.mrl.pixiv.common.datasource.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.mrl.pixiv.common.datasource.local.dao.BrowsingHistoryDao
import com.mrl.pixiv.common.datasource.local.dao.NovelReadLaterDao
import com.mrl.pixiv.common.datasource.local.dao.NovelReadingProgressDao
import com.mrl.pixiv.common.datasource.local.entity.IllustHistoryEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelHistoryEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelReadLaterEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelReadingProgressEntity

@Database(
    entities = [
        IllustHistoryEntity::class,
        NovelHistoryEntity::class,
        NovelReadingProgressEntity::class,
        NovelReadLaterEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@ConstructedBy(AccountDatabaseConstructor::class)
abstract class AccountDatabase : RoomDatabase() {
    abstract fun browsingHistoryDao(): BrowsingHistoryDao
    abstract fun novelReadingProgressDao(): NovelReadingProgressDao
    abstract fun novelReadLaterDao(): NovelReadLaterDao
}

@Suppress("KotlinNoActualForExpect")
expect object AccountDatabaseConstructor : RoomDatabaseConstructor<AccountDatabase> {
    override fun initialize(): AccountDatabase
}
