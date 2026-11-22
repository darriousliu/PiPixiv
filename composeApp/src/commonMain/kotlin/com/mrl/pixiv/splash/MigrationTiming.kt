package com.mrl.pixiv.splash

import co.touchlab.kermit.Severity
import com.mrl.pixiv.common.logs.AppLogStore
import com.mrl.pixiv.common.repository.recovery.ItemStatus
import com.mrl.pixiv.common.repository.recovery.MigrationItem
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.DeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class MigrationTiming(
    val schemaVersion: Int = 2,
    val platform: String,
    val osVersion: String,
    val deviceModel: String?,
    val appVersion: String,
    val outcome: String,
    val items: Map<MigrationItem, ItemStatus> = emptyMap(),
    val degraded: Boolean = false,
    val steps: List<MigrationStep>,
    val totalMs: Long,
    val slowUiShown: Boolean,
    val before: MigrationFiles?,
    val after: MigrationFiles?,
    val peakDiskBytes: Long,
    val availableBytes: Long?,
    val mmkvCounts: Map<String, Long>,
    val memoryStartBytes: Long?,
    val memoryPeakBytes: Long?,
    val memoryWithoutGraphicsStartBytes: Long? = null,
    val memoryWithoutGraphicsPeakBytes: Long? = null,
)

@Serializable
internal data class MigrationStep(val name: String, val elapsedMs: Long, val rows: Long = 0)

@Serializable
internal data class MigrationFiles(
    val globalBytes: Long,
    val walBytes: Long,
    val shmBytes: Long,
    val backupBytes: Long,
    val accountDatabaseBytes: List<Long>,
    val accountSidecarBytes: Long,
    val temporaryBackupBytes: Long,
    val legacyBytes: Long = 0,
) {
    val totalBytes get() = globalBytes + walBytes + shmBytes + backupBytes + temporaryBackupBytes +
        accountDatabaseBytes.sum() + accountSidecarBytes + legacyBytes
}

private val timingJson = Json { encodeDefaults = true }

internal fun writeMigrationTiming(
    outcome: String,
    totalMs: Long,
    slowUiShown: Boolean = false,
    steps: List<MigrationStep> = emptyList(),
    before: MigrationFiles? = null,
    after: MigrationFiles? = null,
    peakDiskBytes: Long = 0,
    availableBytes: Long? = null,
    mmkvCounts: Map<String, Long> = emptyMap(),
    memory: MigrationMemory? = null,
    items: Map<MigrationItem, ItemStatus> = emptyMap(),
    degraded: Boolean = false,
    logTiming: (String) -> Unit,
) {
    val device = DeviceInfo.appLogDetails()
    val timing = MigrationTiming(
        platform = device.platform,
        osVersion = device.osVersion,
        deviceModel = device.model,
        appVersion = AppUtil.versionName,
        outcome = outcome,
        items = items,
        degraded = degraded,
        steps = steps,
        totalMs = totalMs,
        slowUiShown = slowUiShown,
        before = before,
        after = after,
        peakDiskBytes = peakDiskBytes,
        availableBytes = availableBytes,
        mmkvCounts = mmkvCounts,
        memoryStartBytes = memory?.start,
        memoryPeakBytes = memory?.peak,
        memoryWithoutGraphicsStartBytes = memory?.startWithoutGraphics,
        memoryWithoutGraphicsPeakBytes = memory?.peakWithoutGraphics,
    )
    logTiming(migrationTimingPrefix + timingJson.encodeToString(timing))
}

internal fun logMigrationTiming(message: String) =
    AppLogStore.writer.log(Severity.Info, message, migrationTag, null)

internal fun migrationFiles(storage: MigrationStorage): MigrationFiles {
    val path = storage.databasePath()
    val folder = requireNotNull(path.parent)
    val root = storage.accountRoot()
    val accounts = if (SystemFileSystem.exists(root)) SystemFileSystem.list(root) else emptyList()
    val databases = accounts.map { Path(it, "account.db") }.filter(SystemFileSystem::exists)
    fun size(file: Path) = SystemFileSystem.metadataOrNull(file)?.size ?: 0L
    fun global(suffix: String) = size(Path(folder, path.name + suffix))
    return MigrationFiles(
        globalBytes = global(""),
        walBytes = global("-wal"),
        shmBytes = global("-shm"),
        legacyBytes = global(".legacy") + global(".legacy-wal") + global(".legacy-shm"),
        backupBytes = global(".bak") + global("-wal.bak") + global("-shm.bak"),
        temporaryBackupBytes = global(".bak.tmp") + global("-wal.bak.tmp") + global("-shm.bak.tmp"),
        accountDatabaseBytes = databases.map(::size).sorted(),
        accountSidecarBytes = databases.sumOf {
            val parent = requireNotNull(it.parent)
            size(Path(parent, "account.db-wal")) + size(Path(parent, "account.db-shm"))
        },
    )
}

internal class MigrationMemory {
    private val initialSample = migrationMemorySample()
    val start = initialSample?.bytes
    val startWithoutGraphics = initialSample?.bytesWithoutGraphics
    var peak = start
        private set
    var peakWithoutGraphics = startWithoutGraphics
        private set

    fun sample() {
        val sample = migrationMemorySample() ?: return
        peak = maxOf(peak ?: sample.bytes, sample.bytes)
        sample.bytesWithoutGraphics?.let { bytes ->
            peakWithoutGraphics = maxOf(peakWithoutGraphics ?: bytes, bytes)
        }
    }
}

internal data class MigrationMemorySample(val bytes: Long, val bytesWithoutGraphics: Long? = null)

internal expect fun migrationMemorySample(): MigrationMemorySample?

internal class MigrationMetrics(private val storage: MigrationStorage) {
    var before: MigrationFiles? = null
    var after: MigrationFiles? = null
    var available: Long? = null
    var memory: MigrationMemory? = null
    val peakDisk = MutableStateFlow(0L)
    private var sampler: Job? = null

    fun start(scope: CoroutineScope) {
        before = migrationFiles(storage)
        peakDisk.value = before?.totalBytes ?: 0L
        val stats = MigrationMemory().also { memory = it }
        sampler = scope.launch {
            while (true) {
                delay(100)
                stats.sample()
                runCatching { sampleDisk() }
            }
        }
    }

    fun sampleDisk() {
        val files = migrationFiles(storage)
        after = files
        peakDisk.update { maxOf(it, files.totalBytes) }
    }

    suspend fun stop() {
        sampler?.cancelAndJoin()
        if (before != null) {
            runCatching { sampleDisk() }
            memory?.sample()
        }
    }
}
