package com.mrl.pixiv.common.datasource.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.dao.NovelReadingProgressDao
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelReadingProgressEntity

@Database(
    entities = [DownloadEntity::class, NovelReadingProgressEntity::class],
    version = 4,
    exportSchema = false
)
@ConstructedBy(PixivDatabaseConstructor::class)
abstract class PixivDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun novelReadingProgressDao(): NovelReadingProgressDao

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
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS novel_reading_progress (
                        novelId INTEGER NOT NULL,
                        userId INTEGER NOT NULL,
                        paragraphIndex INTEGER NOT NULL,
                        charIndex INTEGER NOT NULL,
                        paragraphHash INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(novelId, userId)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}

@Suppress("KotlinNoActualForExpect")
expect object PixivDatabaseConstructor : RoomDatabaseConstructor<PixivDatabase> {
    override fun initialize(): PixivDatabase
}
