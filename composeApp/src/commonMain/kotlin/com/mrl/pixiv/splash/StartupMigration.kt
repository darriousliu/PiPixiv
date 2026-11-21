package com.mrl.pixiv.splash

import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.datasource.local.AccountDatabaseProvider
import com.mrl.pixiv.common.datasource.local.GlobalDatabaseProvider
import com.mrl.pixiv.common.datasource.local.LegacyAccountDataSplitter
import com.mrl.pixiv.common.datasource.local.LegacyAccountTableGroup
import com.mrl.pixiv.common.datasource.local.LegacyGlobalDatabaseMigration
import com.mrl.pixiv.common.datasource.local.PixivDatabase
import com.mrl.pixiv.common.datasource.local.accountStorageRoot
import com.mrl.pixiv.common.datasource.local.checkpointOversizedWal
import com.mrl.pixiv.common.datasource.local.globalDatabasePath
import com.mrl.pixiv.common.datasource.local.legacyGlobalDatabasePath
import com.mrl.pixiv.common.datasource.local.migration.DatabaseStartupGate
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.mmkv.closeAccountMMKV
import com.mrl.pixiv.common.mmkv.MMKVStorageException
import com.mrl.pixiv.common.mmkv.mmkvOwner
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.LegacyAccountPreferencesMigration
import com.mrl.pixiv.common.repository.LegacyPreferenceOutcome
import com.mrl.pixiv.common.repository.LegacyPreferenceResult
import com.mrl.pixiv.common.repository.prefetchAccountPreferences
import com.mrl.pixiv.common.repository.recovery.ItemStatus
import com.mrl.pixiv.common.repository.recovery.MigrationItem
import com.mrl.pixiv.common.repository.recovery.MigrationReason
import com.mrl.pixiv.common.repository.recovery.isResolved
import com.mrl.pixiv.common.repository.reloadAccountPreferences
import com.mrl.pixiv.common.util.isDebug
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.toKotlinxIoPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import org.koin.core.annotation.Single
import kotlin.time.TimeSource

sealed interface MigrationState {
    data object Idle : MigrationState
    data object Running : MigrationState
    data object Slow : MigrationState
    data class Blocked(val reason: MigrationReason) : MigrationState
    data object Done : MigrationState
}

@Single
class StartupMigration(
    private val database: GlobalDatabaseProvider,
    private val accounts: AccountDatabaseProvider,
) {
    private val lock = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow<MigrationState>(MigrationState.Idle)
    val state = mutableState.asStateFlow()

    private val mutableItems = MutableStateFlow<Map<MigrationItem, ItemStatus>>(pendingItems)
    internal val items = mutableItems.asStateFlow()
    private val mutableHasPending = MutableStateFlow(false)
    internal val hasPending = mutableHasPending.asStateFlow()

    private val slowUiShown = MutableStateFlow(false)
    private var enteredHome = false
    private var cleanupDeferredUntilNextLaunch = false
    private var retryOnHome: Set<MigrationItem>? = null
    internal val storage by lazy {
        val legacy = mmkvOwner("pixiv.user")
        MigrationStorage(
            marker = AccountStore,
            legacy = legacy,
            databasePath = { globalDatabasePath() },
            accountRoot = { accountStorageRoot() },
            filesRoot = { FileKit.filesDir.toKotlinxIoPath() },
            preferences = LegacyAccountPreferencesMigration(legacy = legacy),
            reloadPreferences = { reloadAccountPreferences() },
            observeBlocks = { BlockingRepositoryV2.startObservingDatabase() },
            migrateBlocks = { BlockingRepositoryV2.importLegacyBlocks(BlockingRepositoryV2.readLegacyBlocks(legacy)) },
            debug = isDebug,
        )
    }

    suspend fun run(retry: Boolean = false): Boolean = run(storage, retry)

    internal suspend fun run(
        storage: MigrationStorage,
        retry: Boolean = false,
        logTiming: (String) -> Unit = { logMigrationTiming(it) },
    ): Boolean = withContext(Dispatchers.IO) {
        lock.withLock {
            when (state.value) {
                MigrationState.Done -> true
                is MigrationState.Blocked -> retry && migrate(storage, showImmediately = true, logTiming)
                else -> migrate(storage, showImmediately = retry, logTiming)
            }
        }
    }

    suspend fun awaitReady() = DatabaseStartupGate.awaitReady()

    fun markSlowUiShown() {
        if (state.value == MigrationState.Slow) slowUiShown.value = true
    }

    fun onHomeEntered() {
        scope.launch {
            awaitReady()
            lock.withLock {
                if (enteredHome) return@withLock
                safeStep(storage, "prefetchAccountPreferences") { prefetchAccountPreferences() }
                maintainAfterHome(storage)
            }
        }
    }

    internal suspend fun maintainAfterHome(storage: MigrationStorage): Long? {
        if (enteredHome) return null
        enteredHome = true
        val cleanupMs = accounts.withAccountFilesLocked { cleanAndRetryAfterHome(storage) }

        safeStep(storage, "checkpointGlobalWal") {
            if (!database.requireOpen().checkpointOversizedWal(storage.databasePath())) {
                Logger.w(tag = migrationTag) { "Global database WAL checkpoint deferred until the next launch" }
            }
        }
        accounts.withAccountFilesLocked {
            safeStep(storage, "checkpointAccountWal") {
                val account = accounts.current.value ?: return@safeStep
                if (!account.db.checkpointOversizedWal(accounts.databasePath(account.userId))) {
                    Logger.w(tag = migrationTag) { "Account database WAL checkpoint deferred until the next launch" }
                }
            }
        }
        return cleanupMs
    }

    private suspend fun cleanAndRetryAfterHome(storage: MigrationStorage): Long? {
        val allResolved = items.value.values.all(ItemStatus::isResolved)
        var cleanupMs: Long? = null
        try {
            if (allResolved && !cleanupDeferredUntilNextLaunch && storage.cleanupEligible() && !storage.cleanupDone()) {
                val start = TimeSource.Monotonic.markNow()
                safeStep(storage, "cleanup") {
                    cleanup(storage)
                    storage.markCleanupDone()
                }
                cleanupMs = start.elapsedNow().inWholeMilliseconds
                Logger.i(tag = migrationTag) { "Migration cleanup took ${cleanupMs}ms" }
            }
            val debug = readDebug(storage)
            for (item in retryOnHome.orEmpty()) {
                val status = items.value.getValue(item)
                if (status is ItemStatus.Failed && status.attempts == 1) {
                    migrateItem(storage, item, debug, automatic = true)
                }
            }
            if (items.value.values.all(ItemStatus::isResolved)) storage.markCleanupEligible()
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            logMigrationError(storage, "background maintenance", error)
        }
        return cleanupMs
    }

    internal suspend fun retryItem(
        storage: MigrationStorage,
        item: MigrationItem,
        targetUserId: Long?,
        requireSavedTarget: Boolean = false,
    ): ItemStatus = lock.withLock {
        require(targetUserId == null || targetUserId > 0)
        if (items.value.getValue(item).isResolved) return@withLock items.value.getValue(item)
        val status = accounts.withAccountFilesLocked {
            if (requireSavedTarget && targetUserId != null) {
                check(storage.preferences.sessions.find(targetUserId) != null) { "The recovery target account was removed" }
            }
            migrateItem(storage, item, readDebug(storage), targetUserId, manual = true)
        }
        if (items.value.values.all(ItemStatus::isResolved)) deferCleanup(storage)
        status
    }

    internal suspend fun abandonAll(storage: MigrationStorage) = lock.withLock {
        storeItems(storage, items.value.mapValues { (_, status) ->
            if (status.isResolved) status else ItemStatus.Abandoned
        })
        storage.preferences.sessions.setRecoverAfterLogin(false)
        deferCleanup(storage)
    }

    private fun deferCleanup(storage: MigrationStorage) {
        cleanupDeferredUntilNextLaunch = true
        storage.markCleanupEligible()
    }

    internal suspend fun <T> withRecoveryStorage(block: suspend (PixivDatabase, AccountDatabaseProvider) -> T): T =
        lock.withLock { accounts.withAccountFilesLocked { block(database.requireOpen(), accounts) } }

    private suspend fun migrate(
        storage: MigrationStorage,
        showImmediately: Boolean,
        logTiming: (String) -> Unit,
    ): Boolean {
        val start = TimeSource.Monotonic.markNow()
        slowUiShown.value = false
        mutableState.value = if (showImmediately) MigrationState.Slow else MigrationState.Running
        val steps = mutableListOf<MigrationStep>()
        val counts = mutableMapOf("skippedCorrupted" to 0L)
        val metrics = MigrationMetrics(storage)
        var completed = false
        val outcome = try {
            coroutineScope {
                val slow = launch {
                    delay(500)
                    mutableState.compareAndSet(MigrationState.Running, MigrationState.Slow)
                }
                try {
                    val persisted = storage.readItems()
                    if (retryOnHome == null) {
                        retryOnHome = persisted.filterValues { it is ItemStatus.Failed && it.attempts == 1 }.keys
                    }
                    updateItems(persisted)
                    storage.verifyWritable()
                    storage.preferences.sessions.reloadRecoverAfterLogin()
                    for (userId in storage.preferences.sessions.pendingLocalDeletions) {
                        safeStep(storage, "deleteRemovedAccount") {
                            accounts.closeAndDelete(userId) { closeAccountMMKV(userId) }
                            storage.preferences.sessions.completeLocalDeletion(userId)
                        }
                    }
                    // Inspect even on an already-migrated installation so a damaged account store is retained.
                    storage.preferences.sessions.current
                    val firstAttempt = items.value.getValue(MigrationItem.GlobalDatabase) is ItemStatus.Pending
                    val debug = readDebug(storage)
                    completed = !firstAttempt && items.value.values.none { it is ItemStatus.Pending && it.reason == null }
                    if (!completed) safeStep(storage, "startMetrics") { metrics.start(this) }
                    val pending = items.value.filterValues { it is ItemStatus.Pending && it.reason == null }.keys
                    for (item in preferenceItems) {
                        if (item in pending) migrateItem(storage, item, debug, steps = steps, counts = counts)
                    }
                    if (firstAttempt) {
                        openAndMigrateGlobalDatabase(storage, debug, metrics, steps)
                    } else {
                        steps.measure("openGlobalDatabase") { openUsableGlobalDatabase(storage) }
                    }
                    safeStep(storage, "observeBlocks") {
                        steps.measure("observeBlocks") { storage.observeBlocks() }
                    }
                    for (item in databaseItems) {
                        if (item in pending && items.value.getValue(item) == ItemStatus.Pending()) {
                            migrateItem(storage, item, debug, steps = steps, sampleDisk = metrics::sampleDisk)
                        }
                    }
                    val activeUserId = storage.preferences.sessions.activeUserId.takeIf { it > 0 }
                    safeStep(storage, "reloadPreferences") {
                        steps.measure("reloadPreferences") { storage.reloadPreferences(activeUserId) }
                    }
                    steps.measure("publishAccountDatabase") {
                        accounts.publish(activeUserId, onFailure = {
                            logMigrationError(storage, "publishAccountDatabase", it)
                        })
                    }
                } finally {
                    slow.cancel()
                    metrics.stop()
                }
            }
            DatabaseStartupGate.markReady()
            mutableState.value = MigrationState.Done
            if (completed) "alreadyComplete" else "success"
        } catch (error: Exception) {
            if (error is CancellationException) {
                mutableState.value = MigrationState.Idle
                throw error
            }
            val reason = when (error) {
                is MMKVStorageException, is GlobalStorageUnavailable -> migrationReason(error)
                else -> MigrationReason.Unknown
            }
            mutableState.value = MigrationState.Blocked(reason)
            logStartupError(storage, "startup", error)
            "blocked"
        }
        val totalMs = start.elapsedNow().inWholeMilliseconds
        if (outcome != "alreadyComplete" || storage.debug || totalMs > 20) {
            try {
                writeMigrationTiming(
                    outcome = outcome,
                    totalMs = totalMs,
                    slowUiShown = slowUiShown.value,
                    steps = steps,
                    before = metrics.before,
                    after = metrics.after,
                    peakDiskBytes = metrics.peakDisk.value,
                    availableBytes = metrics.available,
                    mmkvCounts = if (completed) emptyMap() else counts,
                    memory = metrics.memory,
                    items = items.value,
                    degraded = SystemFileSystem.exists(legacyGlobalDatabasePath(storage.databasePath())),
                    logTiming = logTiming,
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                logStartupError(storage, "writeMigrationTiming", error)
            }
        }
        return outcome != "blocked"
    }

    private suspend fun openAndMigrateGlobalDatabase(
        storage: MigrationStorage,
        debug: MigrationDebug,
        metrics: MigrationMetrics,
        steps: MutableList<MigrationStep>,
    ) {
        val path = storage.databasePath()
        val legacy = legacyGlobalDatabasePath(path)
        val savedReason = (items.value.getValue(MigrationItem.TranslationCache) as? ItemStatus.Pending)?.reason
        var degradedReason = savedReason ?: MigrationReason.Corrupted.takeIf { SystemFileSystem.exists(legacy) }
        if (degradedReason == null) {
            try {
                if (SystemFileSystem.exists(path) && storage.checkpoint() < 1) {
                    steps.measure("checkFreeSpace") {
                        val before = metrics.before ?: migrationFiles(storage)
                        val free = debug.availableBytes ?: storage.availableBytes(requireNotNull(path.parent).toString())
                        metrics.available = free
                        val required = 3 * (before.globalBytes + before.walBytes) + 50 * 1024 * 1024
                        if (free < required) throw InsufficientMigrationSpace()
                    }
                    steps.measure("backup") { backupDatabase(path) }
                    storage.markBackupReady()
                    metrics.sampleDisk()
                }
                steps.measure("openGlobalDatabase") {
                    debug.checkFailure(MigrationItem.GlobalDatabase)
                    database.open()
                }
            } catch (error: Exception) {
                if (error is CancellationException || error is MMKVStorageException) throw error
                logMigrationError(storage, "openGlobalDatabase", error)
                degradedReason = fallbackReason(error)
            }
        }
        if (degradedReason != null) {
            setItem(storage, MigrationItem.TranslationCache, ItemStatus.Pending(degradedReason))
            val fallback = try {
                steps.measure("openEmptyGlobalDatabase") { database.openFallback() }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                throw GlobalStorageUnavailable(error)
            }
            if (SystemFileSystem.exists(legacy)) {
                val status = execute(storage, MigrationItem.GlobalDatabase) {
                    steps.measureRows("restoreGlobalTables") {
                        LegacyGlobalDatabaseMigration(fallback).restoreGlobalTables(legacy)
                    }
                    ItemStatus.Done
                }
                setItem(storage, MigrationItem.GlobalDatabase, status)
                return
            }
        }
        setItem(storage, MigrationItem.TranslationCache, ItemStatus.Skipped)
        setItem(storage, MigrationItem.GlobalDatabase, ItemStatus.Done)
    }

    private suspend fun openUsableGlobalDatabase(storage: MigrationStorage) {
        val path = storage.databasePath()
        val legacy = legacyGlobalDatabasePath(path)
        val reason = try {
            if (!SystemFileSystem.exists(path) && SystemFileSystem.exists(legacy)) {
                MigrationReason.Corrupted
            } else {
                database.open()
                return
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            logMigrationError(storage, "reopenGlobalDatabase", error)
            fallbackReason(error)
        }

        try {
            database.openFallback()
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            throw GlobalStorageUnavailable(error)
        }
        if (SystemFileSystem.exists(legacy)) {
            setItem(storage, MigrationItem.TranslationCache, ItemStatus.Pending(reason))
            setItem(storage, MigrationItem.GlobalDatabase, execute(storage, MigrationItem.GlobalDatabase) {
                LegacyGlobalDatabaseMigration(database.requireOpen()).restoreGlobalTables(legacy)
                ItemStatus.Done
            })
        }
    }

    private suspend fun migrateItem(
        storage: MigrationStorage,
        item: MigrationItem,
        debug: MigrationDebug,
        targetUserId: Long? = null,
        manual: Boolean = false,
        automatic: Boolean = false,
        steps: MutableList<MigrationStep> = mutableListOf(),
        counts: MutableMap<String, Long> = mutableMapOf(),
        sampleDisk: () -> Unit = {},
    ): ItemStatus {
        val recovering = manual || automatic
        val result = execute(storage, item, propagateStorageFailure = !recovering) {
            debug.checkFailure(item)
            val allowLegacyUserId = manual || items.value.getValue(MigrationItem.Session) == ItemStatus.Done
            val prefs = storage.preferences
            when (item) {
                MigrationItem.Session -> {
                    val result = steps.preferences(item, counts) {
                        prefs.migrateSession(markRecoverAfterLogin = !recovering)
                    }
                    if (result is ItemStatus.Failed && targetUserId != null && prefs.sessions.find(targetUserId) != null) {
                        ItemStatus.Skipped
                    } else {
                        result
                    }
                }
                MigrationItem.Settings -> steps.preferences(item, counts) {
                    prefs.migrateSettings(targetUserId, allowLegacyUserId)
                }
                MigrationItem.SearchAndTags -> steps.preferences(item, counts) {
                    prefs.migrateSearchAndTags(targetUserId, allowLegacyUserId)
                }
                MigrationItem.SeriesProgress -> steps.preferences(item, counts, prefs::migrateSeriesProgress)
                MigrationItem.BrowsingHistory, MigrationItem.ReadingProgress, MigrationItem.ReadLater -> {
                    val present = LegacyAccountDataSplitter(database.requireOpen(), accounts, legacySource(storage))
                        .split(item.tableGroup, discardedUserIds = prefs.sessions.discardedLocalUserIds) { name, ms, rows ->
                            steps += MigrationStep(name, ms, rows)
                            sampleDisk()
                        }
                    if (present) ItemStatus.Done else ItemStatus.Skipped
                }
                MigrationItem.TranslationCache -> {
                    val source = legacyGlobalDatabasePath(storage.databasePath())
                    if (SystemFileSystem.exists(source) &&
                        LegacyGlobalDatabaseMigration(database.requireOpen()).restoreTranslations(source) != null
                    ) {
                        ItemStatus.Done
                    } else {
                        ItemStatus.Skipped
                    }
                }
                MigrationItem.LegacyBlocking -> {
                    var hadBlocks = false
                    steps.measure(item.name) { hadBlocks = storage.migrateBlocks() }
                    if (hadBlocks) ItemStatus.Done else ItemStatus.Skipped
                }
                MigrationItem.GlobalDatabase -> {
                    LegacyGlobalDatabaseMigration(database.requireOpen()).restoreGlobalTables(legacySource(storage))
                    ItemStatus.Done
                }
            }
        }
        val status = if (recovering && result is ItemStatus.Pending) {
            val previousAttempts = (items.value.getValue(item) as? ItemStatus.Failed)?.attempts ?: 0
            ItemStatus.Failed(previousAttempts + 1, result.reason ?: MigrationReason.Unknown)
        } else {
            result
        }
        setItem(storage, item, status)
        if (recovering && status == ItemStatus.Done && item in preferencesToReload) {
            safeStep(storage, "reloadPreferences") {
                storage.reloadPreferences(storage.preferences.sessions.activeUserId.takeIf { it > 0 })
            }
        }
        return status
    }

    private suspend fun execute(
        storage: MigrationStorage,
        item: MigrationItem,
        propagateStorageFailure: Boolean = true,
        block: suspend () -> ItemStatus,
    ): ItemStatus {
        val previous = items.value.getValue(item)
        val attempts = (previous as? ItemStatus.Failed)?.attempts ?: 0
        return try {
            val status = block()
            if (status is ItemStatus.Failed) status.copy(attempts = attempts + 1) else status
        } catch (error: Exception) {
            if (error is CancellationException || (propagateStorageFailure && error is MMKVStorageException)) throw error
            logMigrationError(storage, item.name, error)
            ItemStatus.Failed(attempts + 1, migrationReason(error))
        }
    }

    private suspend fun cleanup(storage: MigrationStorage) {
        val path = storage.databasePath()
        val global = database.requireOpen()
        LegacyAccountDataSplitter(global, accounts, path).dropLegacyTables()
        for (suffix in databaseSuffixes.reversed()) {
            SystemFileSystem.delete(databaseSibling(backupSidecar(path, suffix), ".tmp"), mustExist = false)
            SystemFileSystem.delete(backupSidecar(path, suffix), mustExist = false)
            SystemFileSystem.delete(databaseSibling(path, ".legacy$suffix"), mustExist = false)
        }
        storage.preferences.clearLegacyUser()
    }

    private fun setItem(storage: MigrationStorage, item: MigrationItem, status: ItemStatus) {
        if (items.value[item] != status) storeItems(storage, items.value + (item to status))
    }

    private fun storeItems(storage: MigrationStorage, values: Map<MigrationItem, ItemStatus>) {
        storage.writeItems(values)
        updateItems(values)
    }

    private fun updateItems(values: Map<MigrationItem, ItemStatus>) {
        mutableItems.value = values
        mutableHasPending.value = values.values.any { !it.isResolved }
    }

    private fun readDebug(storage: MigrationStorage): MigrationDebug {
        if (!storage.debug) return MigrationDebug()
        val path = Path(storage.filesRoot(), "migration-debug.json")
        return try {
            if (!SystemFileSystem.exists(path)) MigrationDebug()
            else SystemFileSystem.source(path).buffered().use { source ->
                require(!source.request(4097)) { "Migration debug configuration is too large" }
                migrationJson.decodeFromString<MigrationDebug>(source.readString())
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            logMigrationError(storage, "readDebug", error)
            MigrationDebug()
        }
    }
}

private val preferenceItems = listOf(
    MigrationItem.Session, MigrationItem.Settings, MigrationItem.SearchAndTags, MigrationItem.SeriesProgress,
)
private val preferencesToReload = setOf(MigrationItem.Settings, MigrationItem.SearchAndTags)
private val databaseItems = listOf(
    MigrationItem.BrowsingHistory, MigrationItem.ReadingProgress, MigrationItem.ReadLater,
    MigrationItem.TranslationCache, MigrationItem.LegacyBlocking,
)

internal val MigrationItem.tableGroup: LegacyAccountTableGroup
    get() = when (this) {
        MigrationItem.BrowsingHistory -> LegacyAccountTableGroup.BrowsingHistory
        MigrationItem.ReadingProgress -> LegacyAccountTableGroup.ReadingProgress
        MigrationItem.ReadLater -> LegacyAccountTableGroup.ReadLater
        else -> error("Migration item does not have account tables")
    }

private fun MigrationDebug.checkFailure(item: MigrationItem) {
    if (item.name in fail) throw SimulatedMigrationFailure(item)
}

private class SimulatedMigrationFailure(item: MigrationItem) : Exception("Injected failure for ${item.name}")
private class GlobalStorageUnavailable(cause: Exception) : Exception("Local database storage unavailable", cause)
private class InsufficientMigrationSpace : Exception("Insufficient storage space")

private fun fallbackReason(error: Exception): MigrationReason =
    if (migrationReason(error) == MigrationReason.StorageSpace) MigrationReason.StorageSpace else MigrationReason.Corrupted

private fun migrationReason(error: Exception): MigrationReason {
    val message = generateSequence<Throwable>(error) { it.cause }.joinToString(" ") { it.message.orEmpty() }
    return when {
        error is InsufficientMigrationSpace || Regex("SQLITE_FULL|disk is full|no space left", RegexOption.IGNORE_CASE).containsMatchIn(message) ->
            MigrationReason.StorageSpace
        Regex("SQLITE_CORRUPT|SQLITE_NOTADB|malformed|not a database", RegexOption.IGNORE_CASE).containsMatchIn(message) ->
            MigrationReason.Corrupted
        error is MMKVStorageException || error is GlobalStorageUnavailable || error is kotlinx.io.IOException ->
            MigrationReason.Io
        else -> MigrationReason.Unknown
    }
}

private fun logStartupError(storage: MigrationStorage, step: String, error: Exception) {
    try {
        logMigrationError(storage, step, error)
    } catch (loggingError: Exception) {
        if (loggingError is CancellationException) throw loggingError
        // The diagnostic writer must not prevent startup from returning.
    }
}

private suspend inline fun safeStep(storage: MigrationStorage, step: String, block: () -> Unit) {
    try {
        block()
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        logMigrationError(storage, step, error)
    }
}

private inline fun MutableList<MigrationStep>.preferences(
    item: MigrationItem,
    counts: MutableMap<String, Long>,
    migrate: () -> LegacyPreferenceResult,
): ItemStatus {
    val start = TimeSource.Monotonic.markNow()
    val result = try {
        migrate()
    } finally {
        add(MigrationStep(item.name, start.elapsedNow().inWholeMilliseconds))
    }
    result.counts.forEach { (key, value) -> counts[key] = (counts[key] ?: 0L) + value }
    return when (result.outcome) {
        LegacyPreferenceOutcome.Done -> ItemStatus.Done
        LegacyPreferenceOutcome.Skipped -> ItemStatus.Skipped
        LegacyPreferenceOutcome.NeedsUser -> ItemStatus.Pending(MigrationReason.Corrupted)
        LegacyPreferenceOutcome.Corrupted -> ItemStatus.Failed(1, MigrationReason.Corrupted)
    }
}

private suspend inline fun <T> MutableList<MigrationStep>.measure(name: String, block: () -> T): T {
    val start = TimeSource.Monotonic.markNow()
    try {
        return block()
    } finally {
        add(MigrationStep(name, start.elapsedNow().inWholeMilliseconds))
    }
}

private suspend inline fun MutableList<MigrationStep>.measureRows(name: String, block: () -> Long) {
    val start = TimeSource.Monotonic.markNow()
    var rows = 0L
    try {
        rows = block()
    } finally {
        add(MigrationStep(name, start.elapsedNow().inWholeMilliseconds, rows))
    }
}
