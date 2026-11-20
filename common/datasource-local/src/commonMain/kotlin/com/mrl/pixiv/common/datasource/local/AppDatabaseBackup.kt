package com.mrl.pixiv.common.datasource.local

import androidx.room3.PooledConnection
import androidx.room3.RoomDatabase
import androidx.room3.Transactor
import androidx.room3.executeSQL
import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

object AppDatabaseBackup {
    suspend fun export(global: PixivDatabase, account: AccountHandle, globalPath: String, accountPath: String) {
        global.snapshotTo(globalPath)
        BundledSQLiteDriver().open(globalPath).use { snapshot ->
            // A deferred split can leave every account's legacy rows in the global database.
            snapshot.execSQL("PRAGMA secure_delete = ON")
            accountTables.forEach { snapshot.execSQL("DROP TABLE IF EXISTS ${it.name}") }
        }

        account.db.snapshotTo(accountPath)
        BundledSQLiteDriver().open(accountPath).use { snapshot ->
            snapshot.execSQL("PRAGMA secure_delete = ON")
            accountTables.forEach { table ->
                snapshot.prepare("DELETE FROM ${table.name} WHERE userId != ?").use { statement ->
                    statement.bindLong(1, account.userId)
                    statement.step()
                }
            }
        }
    }

    suspend fun restore(
        global: PixivDatabase,
        account: AccountHandle,
        globalPath: String,
        accountPath: String,
        backupUserId: Long,
        includeAccountData: Boolean,
    ) {
        require(backupUserId > 0) { "Invalid backup account" }
        global.useWriterConnection { globalConnection ->
            globalConnection.withBackup(globalPath, globalTables) {
                if (includeAccountData) {
                    account.db.useWriterConnection { accountConnection ->
                        accountConnection.withBackup(accountPath, accountTables) {
                            // Validate both snapshots before writing to either database.
                            globalConnection.immediateTransaction {
                                restoreGlobalTables()
                                accountConnection.immediateTransaction {
                                    restoreAccountTables(backupUserId, account.userId)
                                }
                            }
                        }
                    }
                } else {
                    globalConnection.immediateTransaction { restoreGlobalTables() }
                }
            }
        }
    }

    private suspend fun RoomDatabase.snapshotTo(path: String) {
        useWriterConnection { connection ->
            connection.usePrepared("VACUUM INTO ?") { statement ->
                statement.bindText(1, path)
                statement.step()
            }
        }
    }

    private suspend fun Transactor.withBackup(
        path: String,
        expectedTables: List<BackupTable>,
        block: suspend () -> Unit,
    ) {
        val trustedSchema = usePrepared("PRAGMA trusted_schema") { statement ->
            check(statement.step())
            statement.getLong(0)
        }
        executeSQL("PRAGMA trusted_schema = OFF")
        try {
            usePrepared("ATTACH DATABASE ? AS backup") { statement ->
                statement.bindText(1, path)
                statement.step()
            }
            try {
                validateBackup(expectedTables)
                block()
            } finally {
                withContext(NonCancellable) { executeSQL("DETACH DATABASE backup") }
            }
        } finally {
            withContext(NonCancellable) { executeSQL("PRAGMA trusted_schema = $trustedSchema") }
        }
    }

    private suspend fun Transactor.validateBackup(expectedTables: List<BackupTable>) {
        val tables = usePrepared("PRAGMA backup.table_list") { statement ->
            buildSet {
                while (statement.step()) {
                    if (statement.getText(2) == "table") add(statement.getText(1))
                }
            }
        }
        expectedTables.forEach { table ->
            require(table.name in tables) { "Missing backup table ${table.name}" }
            usePrepared("SELECT ${table.columns} FROM backup.${table.name} LIMIT 0") { it.step() }
        }
    }

    private suspend fun PooledConnection.restoreGlobalTables() {
        globalTables.forEach { table ->
            val select = "SELECT ${table.columns} FROM backup.${table.name}"
            val newerTranslation = if (table.name == "novel_translation") {
                """
                WHERE NOT EXISTS (
                    SELECT 1 FROM main.novel_translation AS current
                    WHERE current.novelId = backup.novel_translation.novelId
                        AND current.targetLanguage = backup.novel_translation.targetLanguage
                        AND current.updatedAtMillis > backup.novel_translation.updatedAtMillis
                )
                """.trimIndent()
            } else {
                ""
            }
            executeSQL("INSERT OR REPLACE INTO main.${table.name} (${table.columns}) $select $newerTranslation")
        }
    }

    private suspend fun PooledConnection.restoreAccountTables(backupUserId: Long, userId: Long) {
        accountTables.forEach { table ->
            val projected = table.columns.split(", ").joinToString(", ") { column ->
                when (column) {
                    "userId" -> "?"
                    "state" -> "CASE WHEN state = 'RUNNING' THEN 'PENDING' ELSE state END"
                    "attemptToken" -> "''"
                    else -> column
                }
            }
            val conflict = if (table.name == "novel_read_later") "IGNORE" else "REPLACE"
            usePrepared(
                "INSERT OR $conflict INTO main.${table.name} (${table.columns}) " +
                    "SELECT $projected FROM backup.${table.name} WHERE userId = ?",
            ) { statement ->
                statement.bindLong(1, userId)
                statement.bindLong(2, backupUserId)
                statement.step()
            }
        }
    }

    private data class BackupTable(val name: String, val columns: String)

    private val globalTables = listOf(
        BackupTable(
            "download",
            "illustId, \"index\", title, userId, userName, thumbnailUrl, originalUrl, subFolder, " +
                "status, progress, filePath, fileUri, createTime",
        ),
        BackupTable("block_illust", "illustId, title"),
        BackupTable("block_novel", "novelId, title"),
        BackupTable("block_user", "userId, name"),
        BackupTable("block_tag", "tag, isRegex"),
        BackupTable("block_comment", "commentId, commentJson"),
        BackupTable(
            "novel_translation",
            "novelId, targetLanguage, provider, model, configFingerprint, sourceMd5, translatedText, " +
                "updatedAtMillis, translatedTitle, translatedCaption, metadataSourceMd5",
        ),
    )

    private val accountTables = listOf(
        BackupTable("browsing_history_illust", "illustId, userId, viewedAtMillis, illustJson"),
        BackupTable("browsing_history_novel", "novelId, userId, viewedAtMillis, novelJson"),
        BackupTable(
            "novel_reading_progress",
            "novelId, userId, paragraphIndex, charIndex, paragraphHash, updatedAtMillis",
        ),
        BackupTable(
            "novel_read_later",
            "novelId, userId, targetLanguage, novelTitle, novelCaption, novelAuthorName, coverUrl, " +
                "novelTagsJson, addedAtMillis, provider, model, endpoint, responseApi, extraBody, configFingerprint, " +
                "sourceMd5, state, attemptToken, retryCount, lastError, updatedAtMillis",
        ),
    )
}
