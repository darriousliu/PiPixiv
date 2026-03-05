package com.mrl.pixiv.common.datasource.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity

@Database(entities = [DownloadEntity::class], version = 3, exportSchema = false)
@ConstructedBy(PixivDatabaseConstructor::class)
abstract class PixivDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE download ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE download RENAME COLUMN artist TO userName")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE download ADD COLUMN fileUri TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}

@Suppress("KotlinNoActualForExpect")
expect object PixivDatabaseConstructor : RoomDatabaseConstructor<PixivDatabase> {
    override fun initialize(): PixivDatabase
}
