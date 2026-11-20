package com.mrl.pixiv.splash

import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.datasource.local.AccountDatabaseProvider
import com.mrl.pixiv.common.datasource.local.LegacyAccountDataSplitter
import com.mrl.pixiv.common.datasource.local.PixivDatabase
import com.mrl.pixiv.common.datasource.local.accountStorageRoot
import com.mrl.pixiv.common.datasource.local.globalDatabasePath
import com.mrl.pixiv.common.datasource.local.migration.DatabaseStartupGate
import com.mrl.pixiv.common.datasource.local.openForMigration
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.mmkv.MMKVApp
import com.mrl.pixiv.common.mmkv.MMKVOwner
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.LegacyAccountPreferencesMigration
import com.mrl.pixiv.common.repository.reloadAccountPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.koin.core.annotation.Single
import kotlin.time.TimeSource

sealed interface MigrationState {
    data object Idle : MigrationState
    data object Running : MigrationState
    data object Slow : MigrationState
    data class Failed(val step: MigrationFailure) : MigrationState
    data object Done : MigrationState
}

enum class MigrationFailure {
    StorageSpace, Backup, Preferences, OpenDatabase, SplitAccounts
}

@Single
class StartupMigration(
    private val database: PixivDatabase,
    private val accounts: AccountDatabaseProvider,
) {
    private val lock = Mutex()
    private val mutableState = MutableStateFlow<MigrationState>(MigrationState.Idle)
    val state = mutableState.asStateFlow()
    private val slowUiShown = MutableStateFlow(false)
    private val storage by lazy {
        MigrationStorage(
            marker = MigrationMarker,
            databasePath = ::globalDatabasePath,
            accountRoot = ::accountStorageRoot,
            migratePreferences = LegacyAccountPreferencesMigration::migrate,
            publish = {
                reloadAccountPreferences()
                accounts.publish(AccountStore.activeUserId.takeIf { it > 0 })
                BlockingRepositoryV2.migrate()
                DatabaseStartupGate.markReady()
            },
        )
    }

    suspend fun run(retry: Boolean = false): Boolean = run(storage, retry)

    internal suspend fun run(storage: MigrationStorage, retry: Boolean = false): Boolean =
        withContext(Dispatchers.IO) {
            lock.withLock {
                when (state.value) {
                    MigrationState.Done -> true
                    is MigrationState.Failed -> retry && migrate(storage)
                    else -> migrate(storage)
                }
            }
        }

    suspend fun awaitReady() = DatabaseStartupGate.awaitReady()

    fun markSlowUiShown() {
        if (state.value == MigrationState.Slow) slowUiShown.value = true
    }

    suspend fun continueAfterSplitFailure(): Boolean = withContext(Dispatchers.IO) {
        lock.withLock {
            if (state.value == MigrationState.Done) return@withLock true
            if (state.value != MigrationState.Failed(MigrationFailure.SplitAccounts)) return@withLock false
            publish(storage)
        }
    }

    private suspend fun migrate(storage: MigrationStorage): Boolean = coroutineScope {
        val start = TimeSource.Monotonic.markNow()
        val checkpoint = try {
            storage.marker.kv.getInt(checkpointKey)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            val failure = fail(MigrationFailure.Preferences, error)
            writeMigrationTiming(outcome = failure.name, totalMs = start.elapsedNow().inWholeMilliseconds)
            return@coroutineScope false
        }
        if (checkpoint == complete) {
            val ready = publish(storage)
            if (!ready) writeMigrationTiming(
                outcome = MigrationFailure.OpenDatabase.name,
                totalMs = start.elapsedNow().inWholeMilliseconds,
            )
            return@coroutineScope ready
        }

        slowUiShown.value = false
        mutableState.value = MigrationState.Running
        val slow = launch {
            delay(500)
            mutableState.compareAndSet(MigrationState.Running, MigrationState.Slow)
        }
        val memory = MigrationMemory()
        val peakDisk = MutableStateFlow(0L)
        val sampler = launch {
            while (true) {
                delay(100)
                memory.sample()
                runCatching { migrationFiles(storage).totalBytes }.getOrNull()?.let { bytes ->
                    peakDisk.update { maxOf(it, bytes) }
                }
            }
        }
        var available: Long? = null
        var before: MigrationFiles? = null
        var after: MigrationFiles? = null
        var counts = emptyMap<String, Long>()
        val steps = mutableListOf<MigrationStep>()
        var step = MigrationFailure.Backup
        fun sampleDisk() {
            val files = migrationFiles(storage)
            after = files
            peakDisk.update { maxOf(it, files.totalBytes) }
        }
        val outcome = try {
            val path = storage.databasePath()
            if (checkpoint < backedUp) clearPartialBackup(path)
            before = migrationFiles(storage)
            peakDisk.update { maxOf(it, before.totalBytes) }
            steps.measure("checkFreeSpace") {
                val folder = requireNotNull(path.parent)
                SystemFileSystem.createDirectories(folder)
                val free = storage.availableBytes(folder.toString())
                available = free
                val required = 3 * (before.globalBytes + before.walBytes) + 50 * 1024 * 1024
                if (free < required) throw InsufficientMigrationSpace()
            }
            if (checkpoint < backedUp) {
                steps.measure("backup") { backupDatabase(path) }
                step = MigrationFailure.Preferences
                storage.writeCheckpoint(backedUp)
            }
            sampleDisk()
            step = MigrationFailure.Preferences
            steps.measure("mmkv") { counts = storage.migratePreferences() }
            step = MigrationFailure.OpenDatabase
            steps.measure("openGlobalDatabase") { database.openForMigration() }
            sampleDisk()
            step = MigrationFailure.SplitAccounts
            LegacyAccountDataSplitter(database, accounts, path).split { name, elapsedMs, rows ->
                steps += MigrationStep(name, elapsedMs, rows)
                sampleDisk()
            }
            step = MigrationFailure.Preferences
            storage.writeCheckpoint(complete)
            step = MigrationFailure.OpenDatabase
            storage.publish()
            mutableState.value = MigrationState.Done
            "success"
        } catch (error: Exception) {
            if (error is CancellationException) {
                mutableState.value = MigrationState.Idle
                throw error
            }
            fail(if (error is InsufficientMigrationSpace) MigrationFailure.StorageSpace else step, error).name
        } finally {
            slow.cancel()
            sampler.cancelAndJoin()
        }
        runCatching { sampleDisk() }
        memory.sample()
        writeMigrationTiming(
            outcome = outcome,
            totalMs = start.elapsedNow().inWholeMilliseconds,
            slowUiShown = slowUiShown.value,
            steps = steps,
            before = before,
            after = after,
            peakDiskBytes = peakDisk.value,
            availableBytes = available,
            mmkvCounts = counts,
            memory = memory,
        )
        outcome == "success"
    }

    private suspend fun publish(storage: MigrationStorage): Boolean = try {
        storage.publish()
        mutableState.value = MigrationState.Done
        true
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        fail(MigrationFailure.OpenDatabase, error)
        false
    }

    private fun fail(step: MigrationFailure, error: Exception): MigrationFailure {
        // SQLite errors can include the account database path.
        Logger.e(tag = "StartupMigration") { "Migration failed at ${step.name} (${error::class.simpleName})" }
        mutableState.value = MigrationState.Failed(step)
        return step
    }
}

internal class MigrationStorage(
    val marker: MMKVOwner,
    val databasePath: () -> Path,
    val accountRoot: () -> Path,
    val migratePreferences: () -> Map<String, Long>,
    val publish: suspend () -> Unit,
    val availableBytes: (String) -> Long = ::migrationAvailableBytes,
) {
    fun writeCheckpoint(value: Int) {
        check(marker.kv.set(checkpointKey, value)) { "Unable to store migration checkpoint" }
        marker.sync()
    }
}

private object MigrationMarker : MMKVApp

private const val checkpointKey = "accountStorageMigrationCheckpoint"
private const val backedUp = 1
private const val complete = 2

private class InsufficientMigrationSpace : Exception()

private suspend inline fun MutableList<MigrationStep>.measure(name: String, block: () -> Unit) {
    val start = TimeSource.Monotonic.markNow()
    try {
        block()
    } finally {
        add(MigrationStep(name, start.elapsedNow().inWholeMilliseconds))
    }
}

private fun backupDatabase(path: Path) {
    val folder = requireNotNull(path.parent)
    for (suffix in listOf("", "-wal", "-shm")) {
        val source = Path(folder, path.name + suffix)
        if (!SystemFileSystem.exists(source)) continue
        val backup = Path(folder, path.name + suffix + ".bak")
        val temporary = Path(folder, backup.name + ".tmp")
        try {
            SystemFileSystem.source(source).buffered().use { input ->
                SystemFileSystem.sink(temporary).buffered().use { output -> input.transferTo(output) }
            }
            SystemFileSystem.atomicMove(temporary, backup)
        } finally {
            runCatching { SystemFileSystem.delete(temporary, mustExist = false) }
        }
    }
}

private fun clearPartialBackup(path: Path) {
    val folder = requireNotNull(path.parent)
    for (suffix in listOf("", "-wal", "-shm")) {
        SystemFileSystem.delete(Path(folder, path.name + suffix + ".bak.tmp"), mustExist = false)
    }
}

internal const val migrationTimingPrefix = "MigrationTiming "
