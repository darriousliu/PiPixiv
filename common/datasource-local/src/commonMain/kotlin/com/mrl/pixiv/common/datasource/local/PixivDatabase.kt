package com.mrl.pixiv.common.datasource.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.mrl.pixiv.common.datasource.local.dao.BlockContentDao
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.dao.NovelReadingProgressDao
import com.mrl.pixiv.common.datasource.local.dao.NovelTranslationDao
import com.mrl.pixiv.common.datasource.local.entity.BlockCommentEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockIllustEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockNovelEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockTagEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockUserEntity
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelReadingProgressEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelTranslationEntity

@Database(
    entities = [
        DownloadEntity::class,
        NovelReadingProgressEntity::class,
        NovelTranslationEntity::class,
        BlockIllustEntity::class,
        BlockNovelEntity::class,
        BlockTagEntity::class,
        BlockCommentEntity::class,
        BlockUserEntity::class
    ],
    version = 6,
    exportSchema = false
)
@ConstructedBy(PixivDatabaseConstructor::class)
abstract class PixivDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun novelReadingProgressDao(): NovelReadingProgressDao
    abstract fun novelTranslationDao(): NovelTranslationDao
    abstract fun blockContentDao(): BlockContentDao

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
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS novel_translation (
                        novelId INTEGER NOT NULL,
                        userId INTEGER NOT NULL,
                        targetLanguage TEXT NOT NULL,
                        provider TEXT NOT NULL,
                        model TEXT NOT NULL,
                        sourceMd5 TEXT NOT NULL,
                        translatedText TEXT NOT NULL,
                        updatedAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(novelId, userId, targetLanguage)
                    )
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS block_illust (
                        illustId INTEGER NOT NULL,
                        title TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(illustId)
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS block_novel (
                        novelId INTEGER NOT NULL,
                        title TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(novelId)
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS block_tag (
                        tag TEXT NOT NULL,
                        isRegex INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(tag)
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS block_comment (
                        commentId INTEGER NOT NULL,
                        commentJson TEXT NOT NULL,
                        PRIMARY KEY(commentId)
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS block_user (
                        userId INTEGER NOT NULL,
                        name TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(userId)
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
