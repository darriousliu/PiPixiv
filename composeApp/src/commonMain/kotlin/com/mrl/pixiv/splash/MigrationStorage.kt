package com.mrl.pixiv.splash

import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.datasource.local.legacyGlobalDatabasePath
import com.mrl.pixiv.common.mmkv.MMKVOwner
import com.mrl.pixiv.common.mmkv.MMKVStorageException
import com.mrl.pixiv.common.repository.LegacyAccountPreferencesMigration
import com.mrl.pixiv.common.repository.recovery.ItemStatus
import com.mrl.pixiv.common.repository.recovery.MigrationItem
import com.mrl.pixiv.common.util.availableStorageBytes
import kotlinx.coroutines.CancellationException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
internal val migrationJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    exceptionsWithDebugInfo = false
}
internal val pendingItems = MigrationItem.entries.associateWith { ItemStatus.Pending() }

internal class MigrationStorage(
    val marker: MMKVOwner,
    val legacy: MMKVOwner,
    val databasePath: () -> Path,
    val accountRoot: () -> Path,
    val filesRoot: () -> Path,
    val preferences: LegacyAccountPreferencesMigration,
    val reloadPreferences: (Long?) -> Unit,
    val observeBlocks: suspend () -> Unit,
    val migrateBlocks: suspend () -> Boolean,
    val availableBytes: (String) -> Long = { availableStorageBytes(it) },
    val debug: Boolean,
) {
    fun readItems(): Map<MigrationItem, ItemStatus> {
        val encoded = accessMarker {
            val store = marker.kv
            if (store.containsKey("migrationItems")) store.getString("migrationItems") else null
        }
        if (encoded != null) {
            try {
                return pendingItems + migrationJson.decodeFromString<Map<MigrationItem, ItemStatus>>(encoded)
            } catch (_: IllegalArgumentException) {
                // Re-run idempotent migrations when the item state cannot be read.
            }
        }
        if (checkpoint() == 2) {
            return MigrationItem.entries.associateWith { ItemStatus.Done }.also(::writeItems)
        }
        return if (accessMarker { marker.kv.getBoolean("legacyBlockingMigrated") }) {
            pendingItems + (MigrationItem.LegacyBlocking to ItemStatus.Done)
        } else {
            pendingItems
        }
    }

    fun writeItems(items: Map<MigrationItem, ItemStatus>) =
        writeMarker { marker.kv.set("migrationItems", migrationJson.encodeToString(items)) }

    fun checkpoint(): Int = accessMarker { marker.kv.getInt(checkpointKey) }

    fun markBackupReady() = writeMarker {
        marker.kv.set(checkpointKey, 1) && marker.kv.set(cleanupDoneKey, false)
    }

    fun cleanupEligible(): Boolean = accessMarker { marker.kv.getBoolean(homeReadyKey) }

    fun markCleanupEligible() = writeMarker { marker.kv.set(homeReadyKey, true) }

    fun cleanupDone(): Boolean = accessMarker { marker.kv.getBoolean(cleanupDoneKey) }

    fun markCleanupDone() = writeMarker { marker.kv.set(cleanupDoneKey, true) }

    fun verifyWritable() = accessMarker {
        check(marker.kv.set("migrationStorageReady", true)) { "Unable to write migration state" }
    }

    private inline fun writeMarker(write: () -> Boolean) = accessMarker {
        check(write()) { "Unable to write migration state" }
        marker.sync()
    }

    private inline fun <T> accessMarker(block: () -> T): T = try {
        block()
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        throw MMKVStorageException("Migration state storage is unavailable", error)
    }
}

private const val checkpointKey = "accountStorageMigrationCheckpoint"
private const val homeReadyKey = "accountMigrationHomeReady"
private const val cleanupDoneKey = "accountMigrationCleanupDone"

@Serializable
internal data class MigrationDebug(
    val fail: Set<String> = emptySet(),
    val availableBytes: Long? = null,
)

internal const val migrationTag = "StartupMigration"
internal const val migrationTimingPrefix = "MigrationTiming "

internal fun sanitizedMigrationError(error: Throwable, databaseDirectory: String, filesDirectory: String): String {
    val paths = listOf(databaseDirectory to "<db>", filesDirectory to "<files>").sortedByDescending { it.first.length }
    var message = generateSequence(error) { it.cause }.take(3).joinToString(" | ") {
        "${it::class.simpleName}: ${it.message.orEmpty().lineSequence().firstOrNull().orEmpty()}"
    }
    for ((path, placeholder) in paths) {
        if (path.isNotEmpty()) message = message.replace(path, placeholder)
    }
    return message
        .replace(Regex("""accounts[/\\]\d+(?=[/\\]|\b)"""), "accounts/<id>")
        .take(300)
}

internal fun logMigrationError(storage: MigrationStorage, step: String, error: Throwable) {
    val database = runCatching { storage.databasePath().parent.toString() }.getOrDefault("")
    val files = runCatching { storage.filesRoot().toString() }.getOrDefault("")
    Logger.w(tag = migrationTag) { "Migration $step: ${sanitizedMigrationError(error, database, files)}" }
}

internal val databaseSuffixes = listOf("", "-wal", "-shm")

internal fun databaseSibling(path: Path, suffix: String) = Path(requireNotNull(path.parent), path.name + suffix)

internal fun backupSidecar(path: Path, suffix: String) = databaseSibling(path, "$suffix.bak")

internal fun legacySource(storage: MigrationStorage): Path {
    val path = storage.databasePath()
    return legacyGlobalDatabasePath(path).takeIf(SystemFileSystem::exists) ?: path
}

internal fun backupDatabase(path: Path) {
    for (suffix in databaseSuffixes) {
        val source = databaseSibling(path, suffix)
        val backup = backupSidecar(path, suffix)
        val temporary = databaseSibling(backup, ".tmp")
        try {
            if (SystemFileSystem.exists(source)) {
                copyMigrationFile(source, temporary)
                SystemFileSystem.atomicMove(temporary, backup)
            } else {
                SystemFileSystem.delete(backup, mustExist = false)
            }
        } finally {
            runCatching { SystemFileSystem.delete(temporary, mustExist = false) }
        }
    }
}

internal fun copyMigrationFile(source: Path, target: Path) {
    SystemFileSystem.source(source).buffered().use { input ->
        SystemFileSystem.sink(target).buffered().use { output -> input.transferTo(output) }
    }
}
