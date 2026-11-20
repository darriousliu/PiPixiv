package com.mrl.pixiv.splash

import co.touchlab.kermit.Severity
import com.mrl.pixiv.common.logs.AppLogStore
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.DeviceInfo
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class MigrationTiming(
    val schemaVersion: Int = 1,
    val platform: String,
    val osVersion: String,
    val deviceModel: String?,
    val appVersion: String,
    val outcome: String,
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
) {
    val totalBytes get() = globalBytes + walBytes + shmBytes + backupBytes + temporaryBackupBytes +
        accountDatabaseBytes.sum() + accountSidecarBytes
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
) {
    val device = DeviceInfo.appLogDetails()
    val timing = MigrationTiming(
        platform = device.platform,
        osVersion = device.osVersion,
        deviceModel = device.model,
        appVersion = AppUtil.versionName,
        outcome = outcome,
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
    )
    AppLogStore.writer.log(Severity.Info, migrationTimingPrefix + timingJson.encodeToString(timing), "StartupMigration", null)
}

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
    val start = migrationMemoryBytes()
    var peak = start
        private set

    fun sample() {
        val bytes = migrationMemoryBytes() ?: return
        peak = maxOf(peak ?: bytes, bytes)
    }
}

internal expect fun migrationAvailableBytes(path: String): Long
internal expect fun migrationMemoryBytes(): Long?
