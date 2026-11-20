package com.mrl.pixiv.common.datasource.local

import androidx.room3.executeSQL
import androidx.room3.immediateTransaction
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlin.time.TimeSource

class LegacyAccountDataSplitter(
    private val database: PixivDatabase,
    private val accounts: AccountDatabaseProvider,
    private val sourcePath: Path = globalDatabasePath(),
) {
    suspend fun split(onStep: (name: String, elapsedMs: Long, rows: Long) -> Unit = { _, _, _ -> }) {
        val existingTables = database.useReaderConnection { connection ->
            connection.usePrepared("SELECT name FROM sqlite_master WHERE type = 'table'") { statement ->
                buildSet {
                    while (statement.step()) add(statement.getText(0))
                }
            }
        }
        val legacyTables = tables.filter { it.name in existingTables }
        if (legacyTables.isEmpty()) return

        val migratedAccounts = mutableSetOf<Long>()
        for (table in legacyTables) {
            val start = TimeSource.Monotonic.markNow()
            var rows = 0L
            try {
                for (range in legacyRowRanges(table)) {
                    rows += copyTable(table, range)
                    migratedAccounts += range.userId
                }
            } finally {
                onStep(table.name, start.elapsedNow().inWholeMilliseconds, rows)
            }
        }

        val checkpointStart = TimeSource.Monotonic.markNow()
        // Persist the account WALs before dropping the only live copy of the source tables.
        for (userId in migratedAccounts) {
            accounts.open(userId).db.useWriterConnection { connection ->
                connection.usePrepared("PRAGMA main.wal_checkpoint(FULL)") { statement ->
                    check(statement.step() && statement.getLong(0) == 0L) { "Unable to checkpoint migrated account database" }
                }
            }
        }
        onStep("checkpointAccounts", checkpointStart.elapsedNow().inWholeMilliseconds, 0L)

        val start = TimeSource.Monotonic.markNow()
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                for (table in legacyTables) executeSQL("DROP TABLE ${table.name}")
            }
        }
        onStep("dropLegacyTables", start.elapsedNow().inWholeMilliseconds, 0L)
    }

    private suspend fun legacyRowRanges(table: LegacyTable): List<LegacyRowRange> =
        database.useReaderConnection { connection ->
            connection.usePrepared(
                """
                SELECT userId, MIN(rowid), MAX(rowid) FROM ${table.name}
                WHERE userId > 0 GROUP BY userId ORDER BY userId
                """.trimIndent()
            ) { statement ->
                buildList {
                    while (statement.step()) {
                        add(LegacyRowRange(statement.getLong(0), statement.getLong(1), statement.getLong(2)))
                    }
                }
            }
        }

    private suspend fun copyTable(table: LegacyTable, range: LegacyRowRange): Long =
        accounts.open(range.userId).db.useWriterConnection { connection ->
            connection.usePrepared("ATTACH DATABASE ? AS legacy") { statement ->
                statement.bindText(1, sourcePath.toString())
                statement.step()
            }
            try {
                // Limit the scan to one user's rowid range and avoid re-sorting the account index.
                // SQLite copies the payload directly while Kotlin handles the batch bounds.
                val nextBatchSql = """
                    SELECT COUNT(*), MAX(rowid) FROM (
                        SELECT rowid FROM legacy.${table.name} NOT INDEXED
                        WHERE rowid BETWEEN ? AND ? AND userId = ?
                        ORDER BY rowid LIMIT $BATCH_SIZE
                    )
                """.trimIndent()
                val copySql = """
                    INSERT OR IGNORE INTO main.${table.name} (${table.columns})
                    SELECT ${table.columns} FROM legacy.${table.name} NOT INDEXED
                    WHERE rowid BETWEEN ? AND ? AND userId = ?
                """.trimIndent()
                var totalRows = 0L
                var firstRowId = range.firstRowId
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val (rows, lastRowId) = connection.usePrepared(nextBatchSql) { statement ->
                        statement.bindLong(1, firstRowId)
                        statement.bindLong(2, range.lastRowId)
                        statement.bindLong(3, range.userId)
                        check(statement.step())
                        statement.getLong(0) to statement.getLong(1)
                    }
                    if (rows == 0L) break
                    connection.immediateTransaction {
                        usePrepared(copySql) { statement ->
                            statement.bindLong(1, firstRowId)
                            statement.bindLong(2, lastRowId)
                            statement.bindLong(3, range.userId)
                            statement.step()
                        }
                    }
                    totalRows += rows
                    if (lastRowId == range.lastRowId) break
                    firstRowId = lastRowId + 1
                }
                totalRows
            } finally {
                withContext(NonCancellable) {
                    connection.executeSQL("DETACH DATABASE legacy")
                }
            }
        }

    private data class LegacyTable(val name: String, val columns: String)
    private data class LegacyRowRange(val userId: Long, val firstRowId: Long, val lastRowId: Long)

    private companion object {
        const val BATCH_SIZE = 2_000

        val tables = listOf(
            LegacyTable("browsing_history_illust", "illustId, userId, viewedAtMillis, illustJson"),
            LegacyTable("browsing_history_novel", "novelId, userId, viewedAtMillis, novelJson"),
            LegacyTable(
                "novel_reading_progress",
                "novelId, userId, paragraphIndex, charIndex, paragraphHash, updatedAtMillis",
            ),
            LegacyTable(
                "novel_read_later",
                "novelId, userId, targetLanguage, novelTitle, novelCaption, novelAuthorName, coverUrl, " +
                    "novelTagsJson, addedAtMillis, provider, model, endpoint, responseApi, extraBody, " +
                    "configFingerprint, sourceMd5, state, attemptToken, retryCount, lastError, updatedAtMillis",
            ),
        )
    }
}
