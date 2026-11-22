package com.mrl.pixiv.common.datasource.local

import androidx.room3.RoomDatabase
import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

internal const val databaseWalSizeLimitBytes = 4L * 1024L * 1024L

/** Returns false when the WAL could not be truncated on this attempt. */
suspend fun RoomDatabase.checkpointOversizedWal(path: Path): Boolean {
    val wal = databaseSidecar(path, "-wal")
    if (!wal.exceedsWalSizeLimit()) return true

    // Bound the wait for Room's writer. SQLite does the checkpoint on this background connection.
    val completed = withTimeoutOrNull(250L) {
        useWriterConnection { connection ->
            val busyTimeout = connection.usePrepared("PRAGMA busy_timeout") { statement ->
                check(statement.step())
                statement.getLong(0)
            }
            try {
                // Room installs a busy timeout for regular queries. Maintenance yields to active readers.
                connection.executeSQL("PRAGMA busy_timeout = 0")
                connection.usePrepared("PRAGMA main.wal_checkpoint(TRUNCATE)") { statement ->
                    check(statement.step())
                    statement.getLong(0) == 0L && statement.getLong(1) == statement.getLong(2)
                }
            } finally {
                withContext(NonCancellable) {
                    connection.executeSQL("PRAGMA busy_timeout = $busyTimeout")
                }
            }
        }
    }
    return completed ?: !wal.exceedsWalSizeLimit()
}

private fun Path.exceedsWalSizeLimit(): Boolean =
    (SystemFileSystem.metadataOrNull(this)?.size ?: 0L) > databaseWalSizeLimitBytes
