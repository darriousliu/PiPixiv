package com.mrl.pixiv.common.datasource.local

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.time.Clock

class GlobalDatabaseProvider(private val path: () -> Path = { globalDatabasePath() }) {
    private val mutex = Mutex()
    private val database = MutableStateFlow<PixivDatabase?>(null)

    fun requireOpen(): PixivDatabase = checkNotNull(database.value) { "Global database is not ready" }

    internal suspend fun awaitOpen(): PixivDatabase = database.filterNotNull().first()

    suspend fun open(): PixivDatabase = mutex.withLock { openLocked(path()) }

    suspend fun openFallback(): PixivDatabase = mutex.withLock {
        database.value?.let { return@withLock it }
        val current = path()
        val legacy = legacyGlobalDatabasePath(current)
        resumeCorruptionArchive(current)
        if (SystemFileSystem.exists(legacy) && SystemFileSystem.exists(current)) {
            try {
                return@withLock openLocked(current)
            } catch (error: Exception) {
                if (!error.isDatabaseCorruption()) throw error
                // Keep a previous legacy copy intact if the replacement database is now damaged.
                preserveDatabaseFiles(current, corruptionArchivePath(current))
            }
        } else {
            preserveDatabaseFiles(current, legacy)
        }
        openLocked(current)
    }

    suspend fun close() = mutex.withLock { closeLocked() }

    private suspend fun openLocked(path: Path): PixivDatabase {
        database.value?.let { return it }
        resumeCorruptionArchive(path)
        val candidate = buildGlobalDatabase(path)
        try {
            candidate.openForMigration()
        } catch (error: Throwable) {
            candidate.close()
            throw error
        }
        database.value = candidate
        return candidate
    }

    private fun closeLocked() {
        database.value?.close()
        database.value = null
    }
}

fun legacyGlobalDatabasePath(path: Path): Path = databaseSidecar(path, ".legacy")

fun databaseSidecar(path: Path, suffix: String): Path = Path(requireNotNull(path.parent), path.name + suffix)

internal fun preserveDatabaseFiles(source: Path, destination: Path) {
    // Move the main file first. If interrupted, the next startup can use its new name
    // to resume moving the sidecars before Room opens the now vacant original path.
    val moves = listOf("", "-wal", "-shm").mapNotNull { suffix ->
        val from = databaseSidecar(source, suffix)
        val metadata = SystemFileSystem.metadataOrNull(from) ?: return@mapNotNull null
        check(metadata.isRegularFile) { "Database file is not a regular file" }
        val to = databaseSidecar(destination, suffix)
        check(!SystemFileSystem.exists(to)) { "Database preservation target already exists" }
        from to to
    }
    moves.forEach { (from, to) -> SystemFileSystem.atomicMove(from, to) }
}

internal fun corruptionArchivePath(path: Path): Path = generateSequence(Clock.System.now().toEpochMilliseconds()) { it + 1 }
    .map { databaseSidecar(path, ".corrupt-$it") }
    .first { !SystemFileSystem.exists(it) }

internal fun resumeCorruptionArchive(path: Path) {
    if (SystemFileSystem.exists(path)) return
    if (!SystemFileSystem.exists(databaseSidecar(path, "-wal")) &&
        !SystemFileSystem.exists(databaseSidecar(path, "-shm"))
    ) return

    val prefix = "${path.name}.corrupt-"
    val archive = SystemFileSystem.list(requireNotNull(path.parent))
        .mapNotNull { candidate ->
            if (!candidate.name.startsWith(prefix)) return@mapNotNull null
            candidate.name.removePrefix(prefix).toLongOrNull()?.let { timestamp -> candidate to timestamp }
        }.maxByOrNull { (_, timestamp) -> timestamp }?.first
    if (archive != null) preserveDatabaseFiles(path, archive)
}

internal fun Exception.isDatabaseCorruption(): Boolean = generateSequence<Throwable>(this) { it.cause }.any { cause ->
    val message = cause.message.orEmpty()
    when (cause) {
        is SQLiteException -> message.contains("not a database", ignoreCase = true) ||
            message.contains("malformed", ignoreCase = true) || message.contains("corrupt", ignoreCase = true)

        is IllegalStateException -> message.startsWith("Room cannot verify the data integrity") ||
            message.startsWith("Pre-packaged database has an invalid schema") ||
            message.startsWith("Migration didn't properly handle")

        else -> false
    }
}
