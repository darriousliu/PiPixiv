package com.mrl.pixiv.common.datasource.local

import androidx.room3.executeSQL
import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.time.TimeSource

enum class LegacyAccountTableGroup {
    BrowsingHistory, ReadingProgress, ReadLater,
}

class LegacyAccountDataSplitter(
    private val database: PixivDatabase,
    private val accounts: AccountDatabaseProvider,
    private val sourcePath: Path = globalDatabasePath(),
) {
    suspend fun split(
        item: LegacyAccountTableGroup,
        targetUserId: Long? = null,
        sourceUserId: Long? = null,
        discardedUserIds: Set<Long> = emptySet(),
        onStep: (name: String, elapsedMs: Long, rows: Long) -> Unit = { _, _, _ -> },
    ): Boolean {
        if (!SystemFileSystem.exists(sourcePath)) return false
        var foundTable = false
        var failure: Exception? = null
        for (table in tablesByGroup.getValue(item)) {
            val start = TimeSource.Monotonic.markNow()
            var rows = 0L
            try {
                val (columns, ranges) = readLegacyDatabase(sourcePath) {
                    val columns = columns(table) ?: return@readLegacyDatabase null
                    table.selectColumns(columns, remapUser = targetUserId != null) to rowRanges(table, sourceUserId)
                } ?: continue
                foundTable = true
                for (range in ranges) {
                    if (range.userId in discardedUserIds) continue
                    rows += copyTable(table, columns, range, targetUserId)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (failure == null) failure = error else failure.addSuppressed(error)
            } finally {
                onStep(table.name, start.elapsedNow().inWholeMilliseconds, rows)
            }
        }
        failure?.let { throw it }
        return foundTable
    }

    suspend fun dropLegacyTables() {
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                for (table in legacyAccountTables) executeSQL("DROP TABLE IF EXISTS ${table.name}")
            }
        }
    }

    private suspend fun copyTable(
        table: LegacyTable,
        sourceColumns: String,
        range: LegacyRowRange,
        targetUserId: Long?,
    ): Long = accounts.open(targetUserId ?: range.userId).db.useWriterConnection { connection ->
        val copied = connection.withLegacySource(sourcePath) {
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
                INSERT OR IGNORE INTO main.${table.name} (${table.insertColumns})
                SELECT $sourceColumns FROM legacy.${table.name} NOT INDEXED
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
                        var index = 1
                        targetUserId?.let { statement.bindLong(index++, it) }
                        statement.bindLong(index++, firstRowId)
                        statement.bindLong(index++, lastRowId)
                        statement.bindLong(index, range.userId)
                        statement.step()
                    }
                }
                totalRows += rows
                if (lastRowId == range.lastRowId) break
                firstRowId = lastRowId + 1L
            }
            totalRows
        }
        connection.usePrepared("PRAGMA main.wal_checkpoint(FULL)") { statement ->
            check(statement.step() && statement.getLong(0) == 0L) { "Unable to checkpoint migrated account database" }
        }
        copied
    }

    private companion object {
        const val BATCH_SIZE = 2_000
    }
}

fun legacyAccountUserIds(source: Path): Set<Long> = readLegacyDatabase(source) {
    buildSet {
        legacyAccountTables.forEach { table ->
            val columns = columns(table) ?: return@forEach
            table.selectColumns(columns)
            rowRanges(table).forEach { add(it.userId) }
        }
    }
}

private val tablesByGroup = mapOf(
    LegacyAccountTableGroup.BrowsingHistory to listOf(
        LegacyTable("browsing_history_illust", "illustId, userId, viewedAtMillis, illustJson"),
        LegacyTable("browsing_history_novel", "novelId, userId, viewedAtMillis, novelJson"),
    ),
    LegacyAccountTableGroup.ReadingProgress to listOf(
        LegacyTable(
            "novel_reading_progress",
            "novelId, userId, paragraphIndex, charIndex, paragraphHash, updatedAtMillis",
        ),
    ),
    LegacyAccountTableGroup.ReadLater to listOf(
        LegacyTable(
            "novel_read_later",
            "novelId, userId, targetLanguage, novelTitle, novelCaption, novelAuthorName, coverUrl, " +
                "novelTagsJson, addedAtMillis, provider, model, endpoint, responseApi, extraBody, " +
                "configFingerprint, sourceMd5, state, attemptToken, retryCount, lastError, updatedAtMillis",
        ),
    ),
)

internal val legacyAccountTables = tablesByGroup.values.flatten()
