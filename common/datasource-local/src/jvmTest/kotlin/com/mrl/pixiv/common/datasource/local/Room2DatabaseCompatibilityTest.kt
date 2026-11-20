package com.mrl.pixiv.common.datasource.local

import androidx.room3.useReaderConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelHistoryEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelReadingProgressEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelTranslationEntity
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Room2DatabaseCompatibilityTest {
    @Test
    fun `v9 migration keeps the newest translation for each language and preserves global data`() = runTest {
        withLegacyDatabase(version = 9) { file ->
            BundledSQLiteDriver().open(file.absolutePath).use { connection ->
                connection.execSQL(
                    """
                    INSERT INTO novel_translation
                    SELECT novelId, 2, targetLanguage, provider, model, configFingerprint,
                        sourceMd5, 'Latest translation', 200, translatedTitle, translatedCaption,
                        metadataSourceMd5
                    FROM novel_translation
                    WHERE userId = 1
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    INSERT INTO novel_translation
                    SELECT novelId, 1, 'ja', provider, model, configFingerprint,
                        sourceMd5, translatedText, updatedAtMillis, translatedTitle, translatedCaption,
                        metadataSourceMd5
                    FROM novel_translation
                    WHERE userId = 1
                    """.trimIndent()
                )
                connection.execSQL("INSERT INTO block_user VALUES (2, 'Blocked user')")
            }
            withRoomDatabase(file) { database ->
                assertEquals(
                    legacyTranslation.copy(translatedText = "Latest translation", updatedAtMillis = 200L),
                    database.novelTranslationDao().getByNovelIdAndLanguage(10L, "en"),
                )
                assertEquals(
                    legacyTranslation.copy(targetLanguage = "ja"),
                    database.novelTranslationDao().getByNovelIdAndLanguage(10L, "ja"),
                )
                assertEquals(legacyDownload, database.downloadDao().getDownload(20L, 0))
                assertEquals("Blocked user", database.tableValue("SELECT name FROM block_user"))
                assertEquals("1", database.tableValue("SELECT COUNT(*) FROM browsing_history_novel"))

                database.novelTranslationDao().upsert(legacyTranslation)
                database.downloadDao().updateProgress(20L, 0, 0.75f)
            }
            withRoomDatabase(file) { database ->
                assertEquals(
                    legacyTranslation,
                    database.novelTranslationDao().getByNovelIdAndLanguage(10L, "en"),
                )
                assertEquals(
                    legacyDownload.copy(progress = 0.75f),
                    database.downloadDao().getDownload(20L, 0),
                )
            }
            assertDatabaseVersion(file, 10L)
        }
    }

    @Test
    fun `v8 migration chains through v9 and keeps existing columns and data`() = runTest {
        withLegacyDatabase(version = 8) { file ->
            withRoomDatabase(file) { database ->
                val migratedTranslation = assertNotNull(
                    database.novelTranslationDao().getByNovelIdAndLanguage(10L, "en")
                )
                assertEquals(
                    legacyTranslation.copy(translatedTitle = "", translatedCaption = "", metadataSourceMd5 = ""),
                    migratedTranslation,
                )
                assertEquals(legacyDownload, database.downloadDao().getDownload(20L, 0))
                assertEquals("1", database.tableValue("SELECT COUNT(*) FROM browsing_history_novel"))
                database.novelTranslationDao().upsert(legacyTranslation)
            }

            withRoomDatabase(file) { database ->
                assertEquals(legacyTranslation, database.novelTranslationDao().getByNovelIdAndLanguage(10L, "en"))
            }
            assertDatabaseVersion(file, 10L)
        }
    }

    @Test
    fun `splitter moves valid users and preserves newer account data on retry`() = runTest {
        withLegacyDatabase(version = 9) { file ->
            val path = Path(file.absolutePath)
            val root = file.parentFile.resolve("accounts")
            val provider = AccountDatabaseProvider { Path(root.absolutePath) }
            try {
                BundledSQLiteDriver().open(file.absolutePath).use { connection ->
                    for (uid in listOf(0L, 1L, 2L)) {
                        connection.execSQL("INSERT INTO browsing_history_illust VALUES (10, $uid, 80, '{}')")
                        if (uid != 1L) {
                            connection.execSQL("INSERT INTO browsing_history_novel VALUES (10, $uid, 80, '{}')")
                        }
                        connection.execSQL("INSERT INTO novel_reading_progress VALUES (10, $uid, 1, 2, 3, 80)")
                        connection.execSQL(
                            """
                            INSERT INTO novel_read_later VALUES (
                                10, $uid, 'en', 'Title', '', '', '', '[]', 80, 'OPENAI',
                                'model', 'https://example.com', 0, '', 'config', 'source',
                                'READY', '', 0, NULL, 80
                            )
                            """.trimIndent()
                        )
                    }
                }
                val account1 = provider.open(1L)
                assertNull(provider.current.value)
                val newerHistory = legacyHistory.copy(viewedAtMillis = 120L)
                account1.db.browsingHistoryDao().upsertNovel(newerHistory)
                withRoomDatabase(file) { database ->
                    val splitter = LegacyAccountDataSplitter(database, provider, path)
                    splitter.split()
                    splitter.split()

                    assertEquals(
                        "0",
                        database.tableValue(
                            """
                            SELECT COUNT(*) FROM sqlite_master WHERE name IN (
                                'browsing_history_illust', 'browsing_history_novel',
                                'novel_reading_progress', 'novel_read_later'
                            )
                            """.trimIndent()
                        ),
                    )
                }
                assertNull(provider.current.value)
                assertEquals(setOf("1", "2"), root.list().orEmpty().toSet())
                for (uid in 1L..2L) {
                    val account = provider.open(uid)
                    assertEquals(1, account.db.browsingHistoryDao().countIllusts(uid))
                    assertEquals(1, account.db.browsingHistoryDao().countNovels(uid))
                    assertEquals(
                        NovelReadingProgressEntity(10L, uid, 1, 2, 3, 80L),
                        account.db.novelReadingProgressDao().getByNovelId(uid, 10L),
                    )
                    assertEquals(10L, account.db.novelReadLaterDao().observeByUserId(uid).first().single().novelId)
                }
                assertEquals(newerHistory, account1.db.browsingHistoryDao().getAllNovels(1L).single())
                provider.publish(1L)
                assertEquals(account1, provider.requireCurrent())
            } finally {
                provider.close()
            }
        }
    }

    private suspend fun PixivDatabase.tableValue(sql: String): String = useReaderConnection { connection ->
        connection.usePrepared(sql) { statement ->
            assertTrue(statement.step())
            statement.getText(0)
        }
    }

    private suspend fun withLegacyDatabase(version: Int, block: suspend (File) -> Unit) {
        val directory = Files.createTempDirectory("pixiv-room2-compatibility-").toFile()
        try {
            val file = File(directory, "pixiv_db")
            val schema = checkNotNull(javaClass.getResource("/room2/pixiv-v9.sql")) {
                "Missing frozen Room 2 schema fixture"
            }.readText()
            val connection = BundledSQLiteDriver().open(file.absolutePath)
            try {
                // The fixture intentionally has one complete SQL statement per non-comment line.
                schema.lineSequence()
                    .map(String::trim)
                    .filter { it.isNotEmpty() && !it.startsWith("--") }
                    .forEach(connection::execSQL)
                connection.prepare("SELECT identity_hash FROM room_master_table WHERE id = 42").use {
                    assertTrue(it.step())
                    assertEquals("115367479a96f680e52d2ee7d6dc6421", it.getText(0))
                }
                if (version == 8) {
                    // Reconstruct the version 8 migration precondition from the frozen
                    // Room 2 schema. Only MIGRATION_8_9's three added columns differ.
                    // The historical version 8 identity hash is unknown, so omit it
                    // rather than attribute version 9's hash to a version 8 database.
                    connection.execSQL("ALTER TABLE novel_translation DROP COLUMN translatedTitle")
                    connection.execSQL("ALTER TABLE novel_translation DROP COLUMN translatedCaption")
                    connection.execSQL("ALTER TABLE novel_translation DROP COLUMN metadataSourceMd5")
                    connection.execSQL("DROP TABLE room_master_table")
                    connection.execSQL("PRAGMA user_version = 8")
                }
                connection.execSQL(
                    """
                    INSERT INTO novel_translation (
                        novelId, userId, targetLanguage, provider, model, configFingerprint,
                        sourceMd5, translatedText, updatedAtMillis
                    ) VALUES (10, 1, 'en', 'OPENAI', 'legacy-model', 'legacy-config',
                        'body-md5', 'Saved translation', 100)
                    """.trimIndent()
                )
                if (version == 9) {
                    connection.execSQL(
                        """
                        UPDATE novel_translation SET translatedTitle = 'Saved title',
                            translatedCaption = 'Saved caption', metadataSourceMd5 = 'metadata-md5'
                        WHERE novelId = 10 AND userId = 1 AND targetLanguage = 'en'
                        """.trimIndent()
                    )
                }
                connection.execSQL(
                    """
                    INSERT INTO download (
                        illustId, `index`, title, userId, userName, thumbnailUrl, originalUrl,
                        subFolder, status, progress, filePath, fileUri, createTime
                    ) VALUES (20, 0, 'Saved download', 2, 'Artist', 'https://example.com/thumb.jpg',
                        'https://example.com/original.jpg', NULL, 1, 0.25, '/saved/image.jpg',
                        'file:///saved/image.jpg', 90)
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    INSERT INTO browsing_history_novel (novelId, userId, viewedAtMillis, novelJson)
                    VALUES (10, 1, 80, '{"id":10,"title":"Saved novel"}')
                    """.trimIndent()
                )
            } finally {
                connection.close()
            }
            assertDatabaseVersion(file, version.toLong())
            block(file)
        } finally {
            directory.deleteRecursively()
        }
    }

    private suspend fun withRoomDatabase(file: File, block: suspend (PixivDatabase) -> Unit) {
        val database = buildGlobalDatabase(Path(file.absolutePath))
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    private fun assertDatabaseVersion(file: File, expected: Long) {
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            connection.prepare("PRAGMA user_version").use {
                assertTrue(it.step())
                assertEquals(expected, it.getLong(0))
            }
        } finally {
            connection.close()
        }
    }

    private val legacyTranslation = NovelTranslationEntity(
        novelId = 10L,
        targetLanguage = "en",
        provider = "OPENAI",
        model = "legacy-model",
        configFingerprint = "legacy-config",
        sourceMd5 = "body-md5",
        translatedText = "Saved translation",
        updatedAtMillis = 100L,
        translatedTitle = "Saved title",
        translatedCaption = "Saved caption",
        metadataSourceMd5 = "metadata-md5",
    )

    private val legacyDownload = DownloadEntity(
        illustId = 20L,
        index = 0,
        title = "Saved download",
        userId = 2L,
        userName = "Artist",
        thumbnailUrl = "https://example.com/thumb.jpg",
        originalUrl = "https://example.com/original.jpg",
        status = 1,
        progress = 0.25f,
        filePath = "/saved/image.jpg",
        fileUri = "file:///saved/image.jpg",
        createTime = 90L,
    )

    private val legacyHistory = NovelHistoryEntity(
        novelId = 10L,
        userId = 1L,
        viewedAtMillis = 80L,
        novelJson = """{"id":10,"title":"Saved novel"}""",
    )
}
