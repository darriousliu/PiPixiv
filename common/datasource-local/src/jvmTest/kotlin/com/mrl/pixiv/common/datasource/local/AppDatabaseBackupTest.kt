package com.mrl.pixiv.common.datasource.local

import androidx.room3.executeSQL
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import com.mrl.pixiv.common.datasource.local.entity.BlockUserEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelHistoryEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelTranslationEntity
import java.nio.file.Files
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class AppDatabaseBackupTest {
    @Test
    fun `a failed translation batch keeps restored data and a retry fills the remaining cache`() = runTest(timeout = 15.seconds) {
        val dir = Files.createTempDirectory("pipixiv-backup").toFile()
        val sourceGlobal = buildGlobalDatabase(Path(dir.resolve("source-global.db").absolutePath))
        val sourceAccount = buildAccountDatabase(Path(dir.resolve("source-account.db").absolutePath))
        val targetGlobal = buildGlobalDatabase(Path(dir.resolve("target-global.db").absolutePath))
        val targetAccount = buildAccountDatabase(Path(dir.resolve("target-account.db").absolutePath))
        try {
            val history = NovelHistoryEntity(novelId = 10, userId = 1, viewedAtMillis = 100, novelJson = "{}")
            sourceAccount.browsingHistoryDao().upsertNovels(
                listOf(history, history.copy(novelId = 11, userId = 2)),
            )
            sourceGlobal.blockContentDao().upsertUser(BlockUserEntity(userId = 7, name = "Author"))
            sourceGlobal.useWriterConnection {
                it.executeSQL("CREATE TABLE browsing_history_novel (userId INTEGER, novelJson TEXT)")
                it.executeSQL("INSERT INTO browsing_history_novel VALUES (2, 'private')")
            }
            val translation = NovelTranslationEntity(
                novelId = 20,
                targetLanguage = "en",
                provider = "OPENAI",
                model = "test",
                configFingerprint = "config",
                sourceMd5 = "source",
                translatedText = "old",
                updatedAtMillis = 100,
            )
            for (novelId in 20L..85L) {
                sourceGlobal.novelTranslationDao().upsert(translation.copy(novelId = novelId))
            }
            val newerTranslation = translation.copy(translatedText = "new", updatedAtMillis = 200)
            targetGlobal.novelTranslationDao().upsert(newerTranslation)
            targetGlobal.useWriterConnection {
                it.executeSQL(
                    """
                    CREATE TRIGGER fail_translation_batch BEFORE INSERT ON novel_translation
                    WHEN new.novelId = 85
                    BEGIN
                        SELECT RAISE(ABORT, 'simulated translation batch failure');
                    END
                    """.trimIndent(),
                )
            }

            val globalBackup = dir.resolve("pixiv_db").absolutePath
            val accountBackup = dir.resolve("account.db").absolutePath
            AppDatabaseBackup.export(sourceGlobal, AccountHandle(1, sourceAccount), globalBackup, accountBackup)
            val backupGlobal = buildGlobalDatabase(Path(globalBackup))
            try {
                backupGlobal.useReaderConnection { connection ->
                    val hasLegacyTable = connection.usePrepared(
                        "SELECT 1 FROM sqlite_master WHERE name = 'browsing_history_novel'",
                    ) { it.step() }
                    assertFalse(hasLegacyTable)
                }
            } finally {
                backupGlobal.close()
            }

            val observedHistory = targetAccount.browsingHistoryDao().observeNovelCount(3).produceIn(backgroundScope)
            val observedBlockedUsers = targetGlobal.blockContentDao().observeUsers().produceIn(backgroundScope)
            assertEquals(0, observedHistory.receive())
            assertEquals(emptyList(), observedBlockedUsers.receive())

            val complete = AppDatabaseBackup.restore(
                global = targetGlobal,
                account = AccountHandle(3, targetAccount),
                globalPath = globalBackup,
                accountPath = accountBackup,
                backupUserId = 1,
                includeAccountData = true,
            )
            assertFalse(complete)
            assertEquals(1, observedHistory.receive())
            assertEquals(listOf(BlockUserEntity(7, "Author")), observedBlockedUsers.receive())
            observedHistory.cancel()
            observedBlockedUsers.cancel()
            assertEquals(listOf(history.copy(userId = 3)), targetAccount.browsingHistoryDao().getAllNovels(3))
            assertEquals(newerTranslation, targetGlobal.novelTranslationDao().getByNovelIdAndLanguage(20, "en"))
            assertEquals(translation.copy(novelId = 21), targetGlobal.novelTranslationDao().getByNovelIdAndLanguage(21, "en"))
            assertNull(targetGlobal.novelTranslationDao().getByNovelIdAndLanguage(85, "en"))

            targetGlobal.useWriterConnection { it.executeSQL("DROP TRIGGER fail_translation_batch") }
            assertTrue(
                AppDatabaseBackup.restore(
                    global = targetGlobal,
                    account = AccountHandle(3, targetAccount),
                    globalPath = globalBackup,
                    accountPath = accountBackup,
                    backupUserId = 1,
                    includeAccountData = true,
                ),
            )
            assertEquals(translation.copy(novelId = 85), targetGlobal.novelTranslationDao().getByNovelIdAndLanguage(85, "en"))
            assertEquals(newerTranslation, targetGlobal.novelTranslationDao().getByNovelIdAndLanguage(20, "en"))
        } finally {
            sourceGlobal.close()
            sourceAccount.close()
            targetGlobal.close()
            targetAccount.close()
            dir.deleteRecursively()
        }
    }
}
