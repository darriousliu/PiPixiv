package com.mrl.pixiv.common.datasource.local

import androidx.room3.Transactor
import androidx.room3.executeSQL
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READONLY
import androidx.sqlite.execSQL
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path

internal class LegacyTable(
    val name: String,
    columns: String,
    private val defaults: Map<String, String> = emptyMap(),
) {
    private val columnNames = columns.split(", ")
    val insertColumns = columnNames.joinToString(", ", transform = ::quoteColumn)

    fun selectColumns(sourceColumns: Set<String>, remapUser: Boolean = false): String =
        columnNames.joinToString(", ") { name ->
            when {
                name == "userId" && remapUser -> "?"
                // A previous process cannot finish these read-later attempts. This also handles
                // recovery after the current account's queue has already been initialized.
                name == "state" && name in sourceColumns ->
                    "CASE WHEN state = 'RUNNING' THEN 'PENDING' ELSE state END"
                name == "attemptToken" && name in sourceColumns -> "''"
                name in sourceColumns -> quoteColumn(name)
                name == "userName" && "artist" in sourceColumns -> "artist"
                else -> requireNotNull(defaults[name]) { "Missing legacy column ${this.name}.$name" }
            }
        }
}

private fun quoteColumn(name: String): String = "\"$name\""

internal class LegacyDatabaseSource(private val connection: SQLiteConnection) {
    val version: Long = connection.prepare("PRAGMA user_version").use {
        check(it.step())
        it.getLong(0)
    }

    init {
        require(version in 0..10) { "Unsupported legacy database version $version" }
    }

    private val tables = connection.prepare("PRAGMA main.table_list").use { statement ->
        buildMap {
            while (statement.step()) put(statement.getText(1), statement.getText(2))
        }
    }

    fun columns(table: LegacyTable): Set<String>? {
        val type = tables[table.name] ?: return null
        require(type == "table") { "Invalid legacy table ${table.name}" }
        return connection.prepare("PRAGMA main.table_xinfo(${table.name})").use { statement ->
            buildSet {
                while (statement.step()) {
                    if (statement.getLong(6) == 0L) add(statement.getText(1))
                }
            }
        }
    }

    fun count(table: LegacyTable): Long = connection.prepare("SELECT COUNT(*) FROM ${table.name}").use {
        check(it.step())
        it.getLong(0)
    }

    fun rowRanges(table: LegacyTable, sourceUserId: Long? = null): List<LegacyRowRange> =
        connection.prepare(
            """
            SELECT userId, MIN(rowid), MAX(rowid) FROM ${table.name}
            WHERE userId ${if (sourceUserId == null) "> 0" else "= ?"}
            GROUP BY userId ORDER BY userId
            """.trimIndent()
        ).use { statement ->
            sourceUserId?.let { statement.bindLong(1, it) }
            buildList {
                while (statement.step()) {
                    add(LegacyRowRange(statement.getLong(0), statement.getLong(1), statement.getLong(2)))
                }
            }
        }
}

internal data class LegacyRowRange(val userId: Long, val firstRowId: Long, val lastRowId: Long)

internal fun <T> readLegacyDatabase(path: Path, block: LegacyDatabaseSource.() -> T): T =
    BundledSQLiteDriver().open(path.toString(), SQLITE_OPEN_READONLY).use { connection ->
        connection.execSQL("PRAGMA trusted_schema = OFF")
        LegacyDatabaseSource(connection).block()
    }

internal suspend fun <T> Transactor.withLegacySource(path: Path, block: suspend () -> T): T {
    val trustedSchema = usePrepared("PRAGMA trusted_schema") { statement ->
        check(statement.step())
        statement.getLong(0)
    }
    executeSQL("PRAGMA trusted_schema = OFF")
    try {
        usePrepared("ATTACH DATABASE ? AS legacy") { statement ->
            statement.bindText(1, path.toReadOnlyDatabaseUri())
            statement.step()
        }
        try {
            return block()
        } finally {
            withContext(NonCancellable) { executeSQL("DETACH DATABASE legacy") }
        }
    } finally {
        withContext(NonCancellable) { executeSQL("PRAGMA trusted_schema = $trustedSchema") }
    }
}

internal fun Path.toReadOnlyDatabaseUri(): String {
    val original = toString()
    val normalized = if (original.getOrNull(1) == ':') "/${original.replace('\\', '/')}" else original
    val escaped = normalized.replace("%", "%25").replace("?", "%3F").replace("#", "%23")
    return "file:$escaped?mode=ro"
}
