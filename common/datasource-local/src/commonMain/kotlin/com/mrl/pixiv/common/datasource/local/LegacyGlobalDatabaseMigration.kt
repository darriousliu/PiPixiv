package com.mrl.pixiv.common.datasource.local

import androidx.room3.executeSQL
import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.files.Path

class LegacyGlobalDatabaseMigration(private val database: PixivDatabase) {
    suspend fun restoreGlobalTables(source: Path): Long {
        val projections = readLegacyDatabase(source) {
            globalTables.mapNotNull { table ->
                val columns = columns(table) ?: return@mapNotNull null
                table to table.selectColumns(columns)
            }
        }
        return database.useWriterConnection { connection ->
            connection.withLegacySource(source) {
                connection.immediateTransaction {
                    var rows = 0L
                    for ((table, projection) in projections) {
                        executeSQL(
                            "INSERT OR IGNORE INTO main.${table.name} (${table.insertColumns}) " +
                                "SELECT $projection FROM legacy.${table.name}"
                        )
                        rows += usePrepared("SELECT changes()") { statement ->
                            check(statement.step())
                            statement.getLong(0)
                        }
                    }
                    rows
                }
            }
        }
    }

    suspend fun restoreTranslations(source: Path): Long? {
        val projection = readLegacyDatabase(source) {
            columns(translationTable)?.let { translationTable.selectColumns(it) }
        } ?: return null
        return database.useWriterConnection { connection ->
            connection.withLegacySource(source) {
                val copySql = """
                    INSERT INTO main.novel_translation (${translationTable.insertColumns})
                    SELECT $projection FROM legacy.novel_translation
                    WHERE rowid BETWEEN ? AND ? ORDER BY rowid
                    ON CONFLICT(novelId, targetLanguage) DO UPDATE SET
                        provider = excluded.provider,
                        model = excluded.model,
                        configFingerprint = excluded.configFingerprint,
                        sourceMd5 = excluded.sourceMd5,
                        translatedText = excluded.translatedText,
                        updatedAtMillis = excluded.updatedAtMillis,
                        translatedTitle = excluded.translatedTitle,
                        translatedCaption = excluded.translatedCaption,
                        metadataSourceMd5 = excluded.metadataSourceMd5
                    WHERE excluded.updatedAtMillis >= novel_translation.updatedAtMillis
                """.trimIndent()
                var firstRowId = Long.MIN_VALUE
                var totalRows = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val (rows, lastRowId) = connection.usePrepared(
                        """
                        SELECT COUNT(*), MAX(rowid) FROM (
                            SELECT rowid FROM legacy.novel_translation
                            WHERE rowid >= ? ORDER BY rowid LIMIT 200
                        )
                        """.trimIndent()
                    ) { statement ->
                        statement.bindLong(1, firstRowId)
                        check(statement.step())
                        statement.getLong(0) to statement.getLong(1)
                    }
                    if (rows == 0L) break
                    connection.immediateTransaction {
                        usePrepared(copySql) { statement ->
                            statement.bindLong(1, firstRowId)
                            statement.bindLong(2, lastRowId)
                            statement.step()
                        }
                    }
                    totalRows += rows
                    if (lastRowId == Long.MAX_VALUE) break
                    firstRowId = lastRowId + 1L
                }
                totalRows
            }
        }
    }
}

fun legacyDatabaseCounts(source: Path): Map<String, Long> = readLegacyDatabase(source) {
    (globalTables + translationTable + legacyAccountTables).mapNotNull { table ->
        if (columns(table) == null) null else table.name to count(table)
    }.toMap()
}

private val globalTables = listOf(
    LegacyTable(
        "download",
        "illustId, index, title, userId, userName, thumbnailUrl, originalUrl, subFolder, " +
            "status, progress, filePath, fileUri, createTime",
        mapOf("userId" to "0", "fileUri" to "''"),
    ),
    LegacyTable("block_illust", "illustId, title", mapOf("title" to "''")),
    LegacyTable("block_novel", "novelId, title", mapOf("title" to "''")),
    LegacyTable("block_user", "userId, name", mapOf("name" to "''")),
    LegacyTable("block_tag", "tag, isRegex", mapOf("isRegex" to "0")),
    LegacyTable("block_comment", "commentId, commentJson"),
)

private val translationTable = LegacyTable(
    "novel_translation",
    "novelId, targetLanguage, provider, model, configFingerprint, sourceMd5, translatedText, " +
        "updatedAtMillis, translatedTitle, translatedCaption, metadataSourceMd5",
    mapOf(
        "configFingerprint" to "''",
        "translatedTitle" to "''",
        "translatedCaption" to "''",
        "metadataSourceMd5" to "''",
    ),
)
