package com.mrl.pixiv.common.datasource.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.mrl.pixiv.common.datasource.local.dao.BlockContentDao
import com.mrl.pixiv.common.datasource.local.dao.BrowsingHistoryDao
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.dao.NovelReadLaterDao
import com.mrl.pixiv.common.datasource.local.dao.NovelReadingProgressDao
import com.mrl.pixiv.common.datasource.local.dao.NovelTranslationDao
import com.mrl.pixiv.common.datasource.local.entity.BlockCommentEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockIllustEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockNovelEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockTagEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockUserEntity
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.datasource.local.entity.IllustHistoryEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelHistoryEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelReadLaterEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelReadingProgressEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelTranslationEntity

@Database(
    entities = [
        DownloadEntity::class,
        NovelReadingProgressEntity::class,
        NovelTranslationEntity::class,
        NovelReadLaterEntity::class,
        BlockIllustEntity::class,
        BlockNovelEntity::class,
        BlockTagEntity::class,
        BlockCommentEntity::class,
        BlockUserEntity::class,
        IllustHistoryEntity::class,
        NovelHistoryEntity::class,
    ],
    version = 8,
    exportSchema = false
)
@ConstructedBy(PixivDatabaseConstructor::class)
abstract class PixivDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun novelReadingProgressDao(): NovelReadingProgressDao
    abstract fun novelTranslationDao(): NovelTranslationDao
    abstract fun novelReadLaterDao(): NovelReadLaterDao
    abstract fun blockContentDao(): BlockContentDao
    abstract fun browsingHistoryDao(): BrowsingHistoryDao

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
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS browsing_history_illust (
                        illustId INTEGER NOT NULL,
                        userId INTEGER NOT NULL,
                        viewedAtMillis INTEGER NOT NULL,
                        illustJson TEXT NOT NULL,
                        PRIMARY KEY(illustId, userId)
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_browsing_history_illust_userId_viewedAtMillis
                    ON browsing_history_illust(userId, viewedAtMillis)
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS browsing_history_novel (
                        novelId INTEGER NOT NULL,
                        userId INTEGER NOT NULL,
                        viewedAtMillis INTEGER NOT NULL,
                        novelJson TEXT NOT NULL,
                        PRIMARY KEY(novelId, userId)
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_browsing_history_novel_userId_viewedAtMillis
                    ON browsing_history_novel(userId, viewedAtMillis)
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE novel_translation_new (
                        novelId INTEGER NOT NULL,
                        userId INTEGER NOT NULL,
                        targetLanguage TEXT NOT NULL,
                        provider TEXT NOT NULL,
                        model TEXT NOT NULL,
                        configFingerprint TEXT NOT NULL,
                        sourceMd5 TEXT NOT NULL,
                        translatedText TEXT NOT NULL,
                        updatedAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(novelId, userId, targetLanguage)
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    INSERT INTO novel_translation_new (
                        novelId,
                        userId,
                        targetLanguage,
                        provider,
                        model,
                        configFingerprint,
                        sourceMd5,
                        translatedText,
                        updatedAtMillis
                    )
                    SELECT
                        novelId,
                        userId,
                        targetLanguage,
                        provider,
                        model,
                        '',
                        sourceMd5,
                        translatedText,
                        updatedAtMillis
                    FROM novel_translation
                    """.trimIndent()
                )
                connection.execSQL("DROP TABLE novel_translation")
                connection.execSQL(
                    "ALTER TABLE novel_translation_new RENAME TO novel_translation"
                )
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS novel_read_later (
                        novelId INTEGER NOT NULL,
                        userId INTEGER NOT NULL,
                        targetLanguage TEXT NOT NULL,
                        novelTitle TEXT NOT NULL,
                        novelCaption TEXT NOT NULL,
                        novelAuthorName TEXT NOT NULL,
                        coverUrl TEXT NOT NULL,
                        novelTagsJson TEXT NOT NULL,
                        addedAtMillis INTEGER NOT NULL,
                        provider TEXT NOT NULL,
                        model TEXT NOT NULL,
                        endpoint TEXT NOT NULL,
                        responseApi INTEGER NOT NULL,
                        extraBody TEXT NOT NULL,
                        configFingerprint TEXT NOT NULL,
                        sourceMd5 TEXT NOT NULL,
                        state TEXT NOT NULL,
                        attemptToken TEXT NOT NULL,
                        retryCount INTEGER NOT NULL,
                        lastError TEXT,
                        updatedAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(novelId, userId, targetLanguage)
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_novel_read_later_userId_state_addedAtMillis
                    ON novel_read_later(userId, state, addedAtMillis)
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
