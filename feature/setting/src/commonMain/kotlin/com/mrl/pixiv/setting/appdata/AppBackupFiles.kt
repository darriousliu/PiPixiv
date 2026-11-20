package com.mrl.pixiv.setting.appdata

import com.mrl.pixiv.common.datasource.local.AccountHandle
import com.mrl.pixiv.common.datasource.local.AppDatabaseBackup
import com.mrl.pixiv.common.datasource.local.DatabaseBackupTiming
import com.mrl.pixiv.common.datasource.local.PixivDatabase
import com.mrl.pixiv.common.util.BackupArchive
import com.mrl.pixiv.common.util.BackupStep
import com.mrl.pixiv.common.util.BackupTiming
import com.mrl.pixiv.common.util.ZipUtil
import com.mrl.pixiv.common.util.availableStorageBytes
import com.mrl.pixiv.common.util.checkBackupExportSpace
import com.mrl.pixiv.common.util.measureBackupStep
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.toKotlinxIoPath
import io.github.vinceglb.filekit.write
import io.github.vinceglb.filekit.writeString

private const val jsonDataFile = "data.json"
private const val backupDirectory = "pipixiv-backup"
private const val globalDatabaseFile = "pixiv_db"
private const val accountDatabaseFile = "account.db"
private const val currentJsonLimit = 16 * 1024 * 1024
private const val legacyJsonLimit = 128 * 1024 * 1024

sealed interface AppBackupContents {
    data class V4(
        val metadata: String,
        val globalPath: String,
        val accountPath: String,
    ) : AppBackupContents

    data class Legacy(val metadata: String) : AppBackupContents
}

class AppBackupFiles(
    private val zipUtil: ZipUtil,
    private val availableBytes: (String) -> Long = ::availableStorageBytes,
) {
    suspend fun exportV4(
        metadata: String,
        global: PixivDatabase,
        account: AccountHandle,
        globalDatabasePath: String,
        accountDatabasePath: String,
        workDir: PlatformFile,
        destination: PlatformFile,
        timing: BackupTiming? = null,
        databaseTiming: DatabaseBackupTiming? = null,
    ) {
        checkBackupExportSpace(workDir, listOf(globalDatabasePath, accountDatabasePath), availableBytes)
        val source = (workDir / backupDirectory).also { it.createDirectories() }
        (source / jsonDataFile).writeString(metadata)
        AppDatabaseBackup.export(
            global = global,
            account = account,
            globalPath = (source / globalDatabaseFile).toKotlinxIoPath().toString(),
            accountPath = (source / accountDatabaseFile).toKotlinxIoPath().toString(),
            timing = databaseTiming,
        )
        val archive = workDir / "backup.zip"
        timing.measureBackupStep(BackupStep.Compress) {
            check(zipUtil.compress(source.absolutePath(), archive.absolutePath())) {
                "Unable to create backup ZIP"
            }
        }
        timing.measureBackupStep(BackupStep.CopyToDestination) {
            destination.write(archive)
        }
    }

    suspend fun read(
        source: PlatformFile,
        workDir: PlatformFile,
        timing: BackupTiming? = null,
    ): AppBackupContents {
        val archive = BackupArchive.open(source, workDir / "backup.zip", zipUtil, availableBytes, timing)
        val currentMetadata = archive.read("$backupDirectory/$jsonDataFile", currentJsonLimit)
        if (currentMetadata != null) {
            val globalSnapshot = workDir / globalDatabaseFile
            val accountSnapshot = workDir / accountDatabaseFile
            timing.measureBackupStep(BackupStep.ExtractSnapshots) {
                archive.extract("$backupDirectory/$globalDatabaseFile", globalSnapshot)
                archive.extract("$backupDirectory/$accountDatabaseFile", accountSnapshot)
            }
            return AppBackupContents.V4(
                metadata = currentMetadata.decodeToString(),
                globalPath = globalSnapshot.toKotlinxIoPath().toString(),
                accountPath = accountSnapshot.toKotlinxIoPath().toString(),
            )
        }
        val legacyMetadata = checkNotNull(archive.read(jsonDataFile, legacyJsonLimit)) {
            "No data.json in ZIP file"
        }
        return AppBackupContents.Legacy(legacyMetadata.decodeToString())
    }
}

suspend fun confirmBackupAccountImport(
    backupUserId: Long,
    currentUserId: Long,
    confirm: suspend (currentUserId: Long, backupUserId: Long) -> Boolean,
): Boolean = backupUserId == currentUserId || confirm(currentUserId, backupUserId)
