package com.mrl.pixiv.common.datasource.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.mrl.pixiv.common.datasource.local.entity.NovelReadLaterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NovelReadLaterDatabaseTest {
    private lateinit var database: PixivDatabase

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<PixivDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun `dao isolates composite keys and performs state transitions`() = runTest {
        val dao = database.novelReadLaterDao()
        val first = entity(userId = 1L, novelId = 10L, targetLanguage = "en")
        val anotherLanguage = entity(userId = 1L, novelId = 10L, targetLanguage = "ja")
        val anotherUser = entity(userId = 2L, novelId = 10L, targetLanguage = "en")
        dao.insert(first)
        dao.insert(anotherLanguage)
        dao.insert(anotherUser)

        assertEquals(2, dao.observeByUserId(1L).first().size)
        assertEquals(1, dao.observeByUserId(2L).first().size)
        assertEquals(
            setOf("en", "ja"),
            dao.getPending(1L).map { it.targetLanguage }.toSet(),
        )

        assertEquals(
            1,
            dao.claimPending(
                userId = 1L,
                novelId = 10L,
                targetLanguage = "en",
                attemptToken = "attempt-1",
                updatedAtMillis = 2L,
            )
        )
        assertEquals(
            0,
            dao.claimPending(
                userId = 1L,
                novelId = 10L,
                targetLanguage = "en",
                attemptToken = "attempt-2",
                updatedAtMillis = 3L,
            )
        )
        assertEquals(
            "RUNNING",
            dao.getByKey(1L, 10L, "en")?.state,
        )

        dao.updateResult(
            userId = 1L,
            novelId = 10L,
            targetLanguage = "en",
            attemptToken = "attempt-1",
            state = "READY",
            retryCount = 1,
            lastError = null,
            sourceMd5 = "source",
            updatedAtMillis = 4L,
        )
        val ready = dao.getByKey(1L, 10L, "en")
        assertEquals("READY", ready?.state)
        assertEquals("", ready?.attemptToken)
        assertEquals("source", ready?.sourceMd5)
        assertEquals(1, ready?.retryCount)
        assertEquals("""["short","a/b"]""", ready?.novelTagsJson)

        dao.deleteByKey(1L, 10L, "en")
        assertNull(dao.getByKey(1L, 10L, "en"))
        assertNotNull(dao.getByKey(1L, 10L, "ja"))
        assertNotNull(dao.getByKey(2L, 10L, "en"))
    }

    @Test
    fun `restore interrupted only resets running work`() = runTest {
        val dao = database.novelReadLaterDao()
        dao.insert(entity(novelId = 1L, state = "RUNNING"))
        dao.insert(entity(novelId = 2L, state = "READY"))
        dao.insert(entity(novelId = 3L, state = "FAILED"))

        dao.restoreInterrupted(updatedAtMillis = 99L)

        assertEquals("PENDING", dao.getByKey(1L, 1L, "en")?.state)
        assertEquals(99L, dao.getByKey(1L, 1L, "en")?.updatedAtMillis)
        assertEquals("READY", dao.getByKey(1L, 2L, "en")?.state)
        assertEquals("FAILED", dao.getByKey(1L, 3L, "en")?.state)
    }

    @Test
    fun `stale attempt cannot overwrite a reinserted queue row`() = runTest {
        val dao = database.novelReadLaterDao()
        dao.insert(entity(novelId = 10L))
        assertEquals(
            1,
            dao.claimPending(
                userId = 1L,
                novelId = 10L,
                targetLanguage = "en",
                attemptToken = "old-attempt",
                updatedAtMillis = 2L,
            )
        )

        dao.deleteByKey(1L, 10L, "en")
        dao.insert(entity(novelId = 10L))
        assertEquals(
            1,
            dao.claimPending(
                userId = 1L,
                novelId = 10L,
                targetLanguage = "en",
                attemptToken = "new-attempt",
                updatedAtMillis = 3L,
            )
        )

        assertEquals(
            0,
            dao.updateResult(
                userId = 1L,
                novelId = 10L,
                targetLanguage = "en",
                attemptToken = "old-attempt",
                state = "READY",
                retryCount = 0,
                lastError = null,
                sourceMd5 = "stale-source",
                updatedAtMillis = 4L,
            )
        )
        dao.restoreRunningAttempt(
            userId = 1L,
            novelId = 10L,
            targetLanguage = "en",
            attemptToken = "old-attempt",
            updatedAtMillis = 5L,
        )
        val reinserted = dao.getByKey(1L, 10L, "en")
        assertEquals("RUNNING", reinserted?.state)
        assertEquals("", reinserted?.sourceMd5)
        assertEquals("new-attempt", reinserted?.attemptToken)
    }

    @Test
    fun `duplicate enqueue cannot replace a running claim`() = runTest {
        val dao = database.novelReadLaterDao()
        dao.insert(entity(novelId = 10L))
        dao.claimPending(
            userId = 1L,
            novelId = 10L,
            targetLanguage = "en",
            attemptToken = "active-attempt",
            updatedAtMillis = 2L,
        )

        dao.insert(entity(novelId = 10L))

        val running = dao.getByKey(1L, 10L, "en")
        assertEquals("RUNNING", running?.state)
        assertEquals("active-attempt", running?.attemptToken)
    }

    @Test
    fun `pending query keeps blocked and claimable endpoints in queue order`() = runTest {
        val dao = database.novelReadLaterDao()
        dao.insert(
            entity(
                novelId = 1L,
                endpoint = "http://192.168.1.20:11434",
            )
        )
        dao.insert(
            entity(
                novelId = 2L,
                endpoint = "https://api.example.com",
            )
        )

        assertEquals(
            listOf(
                "http://192.168.1.20:11434",
                "https://api.example.com",
            ),
            dao.getPending(1L).map { it.endpoint },
        )
    }

    @Test
    fun `invalid ready cache becomes retryable`() = runTest {
        val dao = database.novelReadLaterDao()
        dao.insert(entity(novelId = 10L, state = "READY"))

        assertEquals(
            1,
            dao.invalidateReady(
                userId = 1L,
                novelId = 10L,
                targetLanguage = "en",
                lastError = "cache mismatch",
                updatedAtMillis = 2L,
            )
        )
        val invalidated = dao.getByKey(1L, 10L, "en")
        assertEquals("FAILED", invalidated?.state)
        assertEquals("cache mismatch", invalidated?.lastError)
    }

    @Test
    fun `migration 7 to 8 creates queue table and index`() {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.execSQL(
                """
                CREATE TABLE novel_translation (
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
            connection.execSQL(
                """
                INSERT INTO novel_translation VALUES (
                    10, 1, 'en', 'OPENAI', 'model', 'source', 'translated', 1
                )
                """.trimIndent()
            )
            PixivDatabase.MIGRATION_7_8.migrate(connection)

            val columns = buildSet {
                connection.prepare("PRAGMA table_info(novel_read_later)").use { statement ->
                    while (statement.step()) {
                        add(statement.getText(1))
                    }
                }
            }
            assertTrue(
                columns.containsAll(
                    setOf(
                        "novelId",
                        "userId",
                        "targetLanguage",
                        "novelTitle",
                        "novelCaption",
                        "novelAuthorName",
                        "coverUrl",
                        "novelTagsJson",
                        "addedAtMillis",
                        "provider",
                        "model",
                        "endpoint",
                        "responseApi",
                        "extraBody",
                        "configFingerprint",
                        "sourceMd5",
                        "state",
                        "attemptToken",
                        "retryCount",
                        "lastError",
                        "updatedAtMillis",
                    )
                )
            )

            val indices = buildSet {
                connection.prepare("PRAGMA index_list(novel_read_later)").use { statement ->
                    while (statement.step()) {
                        add(statement.getText(1))
                    }
                }
            }
            assertTrue("index_novel_read_later_userId_state_addedAtMillis" in indices)
            assertFalse(columns.any { it.equals("apiKey", ignoreCase = true) })

            val translationColumns = buildSet {
                connection.prepare("PRAGMA table_info(novel_translation)").use { statement ->
                    while (statement.step()) {
                        add(statement.getText(1))
                    }
                }
            }
            assertTrue("configFingerprint" in translationColumns)
            connection.prepare(
                "SELECT configFingerprint FROM novel_translation WHERE novelId = 10"
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("", statement.getText(0))
            }
        } finally {
            connection.close()
        }
    }

    @Test
    fun `migration 8 to 9 preserves body cache and defaults metadata fields`() {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.execSQL(
                """
                CREATE TABLE novel_translation (
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
                INSERT INTO novel_translation VALUES (
                    10, 1, 'en', 'OPENAI', 'model', 'fingerprint', 'body-source',
                    'translated body', 1
                )
                """.trimIndent()
            )

            PixivDatabase.MIGRATION_8_9.migrate(connection)

            val columns = buildSet {
                connection.prepare("PRAGMA table_info(novel_translation)").use { statement ->
                    while (statement.step()) {
                        add(statement.getText(1))
                    }
                }
            }
            assertTrue(
                columns.containsAll(
                    setOf(
                        "translatedTitle",
                        "translatedCaption",
                        "metadataSourceMd5",
                    )
                )
            )
            connection.prepare(
                """
                SELECT configFingerprint, sourceMd5, translatedText, updatedAtMillis,
                    translatedTitle, translatedCaption, metadataSourceMd5
                FROM novel_translation
                WHERE novelId = 10 AND userId = 1 AND targetLanguage = 'en'
                """.trimIndent()
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("fingerprint", statement.getText(0))
                assertEquals("body-source", statement.getText(1))
                assertEquals("translated body", statement.getText(2))
                assertEquals(1L, statement.getLong(3))
                assertEquals("", statement.getText(4))
                assertEquals("", statement.getText(5))
                assertEquals("", statement.getText(6))
            }

            connection.execSQL(
                """
                INSERT INTO novel_translation (
                    novelId,
                    userId,
                    targetLanguage,
                    provider,
                    model,
                    configFingerprint,
                    sourceMd5,
                    translatedText,
                    updatedAtMillis
                ) VALUES (
                    11, 1, 'en', 'OPENAI', 'model', 'fingerprint', 'new-body-source',
                    'new translated body', 2
                )
                """.trimIndent()
            )
            connection.prepare(
                """
                SELECT translatedTitle, translatedCaption, metadataSourceMd5
                FROM novel_translation
                WHERE novelId = 11 AND userId = 1 AND targetLanguage = 'en'
                """.trimIndent()
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("", statement.getText(0))
                assertEquals("", statement.getText(1))
                assertEquals("", statement.getText(2))
            }
        } finally {
            connection.close()
        }
    }

    private fun entity(
        userId: Long = 1L,
        novelId: Long,
        targetLanguage: String = "en",
        state: String = "PENDING",
        endpoint: String = "https://example.com/v1",
    ) = NovelReadLaterEntity(
        novelId = novelId,
        userId = userId,
        targetLanguage = targetLanguage,
        novelTitle = "Novel $novelId",
        novelCaption = "Summary",
        novelAuthorName = "Author",
        coverUrl = "https://example.com/cover.jpg",
        novelTagsJson = """["short","a/b"]""",
        addedAtMillis = novelId,
        provider = "OPENAI",
        model = "model",
        endpoint = endpoint,
        responseApi = false,
        extraBody = "",
        configFingerprint = "fingerprint",
        sourceMd5 = "",
        state = state,
        attemptToken = if (state == "RUNNING") "attempt-$novelId" else "",
        retryCount = 0,
        lastError = null,
        updatedAtMillis = 1L,
    )
}
