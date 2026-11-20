package com.mrl.pixiv.splash

import com.mrl.pixiv.common.datasource.local.LegacyAccountDataSplitter
import com.mrl.pixiv.common.datasource.local.LegacyGlobalDatabaseMigration
import com.mrl.pixiv.common.datasource.local.legacyAccountUserIds
import com.mrl.pixiv.common.datasource.local.legacyDatabaseCounts
import com.mrl.pixiv.common.datasource.local.legacyGlobalDatabasePath
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.LegacyBlocks
import com.mrl.pixiv.common.repository.LegacyPreferenceOutcome
import com.mrl.pixiv.common.repository.LegacyPreferencesPackage
import com.mrl.pixiv.common.repository.recovery.DataRecoveryRepository
import com.mrl.pixiv.common.repository.recovery.ItemStatus
import com.mrl.pixiv.common.repository.recovery.MigrationItem
import com.mrl.pixiv.common.util.BackupArchive
import com.mrl.pixiv.common.util.BackupSpaceException
import com.mrl.pixiv.common.util.ZipUtil
import com.mrl.pixiv.common.util.deleteRecursively
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.toKotlinxIoPath
import io.github.vinceglb.filekit.write
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single(binds = [DataRecoveryRepository::class])
class StartupDataRecoveryRepository(
    private val migration: StartupMigration,
    private val zipUtil: ZipUtil,
) : DataRecoveryRepository {
    override val items get() = migration.items
    override val hasPending get() = migration.hasPending
    override val recoverAfterLogin get() = migration.storage.preferences.sessions.recoverAfterLogin

    override suspend fun needsTargetAccount(item: MigrationItem): Boolean = withContext(Dispatchers.IO) {
        migration.awaitReady()
        item in legacyOwnerItems && migration.storage.preferences.legacyUserId() == null
    }

    override suspend fun retry(item: MigrationItem, targetUserId: Long?) = withContext(Dispatchers.IO) {
        migration.awaitReady()
        migration.retryItem(migration.storage, item, targetUserId, requireSavedTarget = true)
    }

    override suspend fun exportLegacyData(target: PlatformFile) = withContext(Dispatchers.IO) {
        migration.awaitReady()
        exportLegacyData(migration.storage, target)
    }

    internal suspend fun exportLegacyData(
        storage: MigrationStorage,
        target: PlatformFile,
        readBlocks: () -> LegacyBlocks = { BlockingRepositoryV2.readLegacyBlocks(storage.legacy) },
    ) {
        migration.withRecoveryStorage { _, _ ->
            withWorkDirectory { work ->
                val root = (work / archiveDirectory).also { it.createDirectories() }
                val mmkv = LegacyPackagePreferences(storage.preferences.readPackage(), readBlocks())
                val path = storage.databasePath()
                val legacyPath = legacyGlobalDatabasePath(path)
                val hasLegacy = SystemFileSystem.exists(legacyPath)
                val sourceName = when {
                    hasLegacy -> legacyPath.name
                    SystemFileSystem.exists(backupSidecar(path, "")) -> backupSidecar(path, "").name
                    else -> null
                }
                var databaseCounts = emptyMap<String, Long>()
                checkArchiveExportSpace(work, storage, path, legacyPath, hasLegacy)
                if (sourceName != null) {
                    val packagedDatabase = (root / archiveDatabase).toKotlinxIoPath()
                    for (suffix in databaseSuffixes) {
                        val source = if (hasLegacy) databaseSibling(legacyPath, suffix) else backupSidecar(path, suffix)
                        if (SystemFileSystem.exists(source)) {
                            copyMigrationFile(source, databaseSibling(packagedDatabase, suffix))
                        }
                    }
                    databaseCounts = try {
                        legacyDatabaseCounts(packagedDatabase)
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        logMigrationError(storage, "countLegacyArchive", error)
                        emptyMap()
                    }
                }
                val manifest = LegacyPackageManifest(
                    sourceDatabase = sourceName,
                    counts = databaseCounts + mmkv.preferences.counts() + mmkv.blocks.counts(),
                )
                (root / archiveManifest).writeString(migrationJson.encodeToString(manifest))
                (root / archiveMetadata).writeString(migrationJson.encodeToString(mmkv))
                val archive = work / "legacy.zip"
                check(zipUtil.compress(root.absolutePath(), archive.absolutePath())) { "Unable to create legacy archive" }
                target.write(archive)
            }
        }
    }

    override suspend fun importLegacyPackage(source: PlatformFile, targetUserId: Long) = withContext(Dispatchers.IO) {
        migration.awaitReady()
        importLegacyPackage(migration.storage, source, targetUserId)
    }

    override suspend fun importLegacyPackage(
        source: PlatformFile,
        targetUserId: Long,
        item: MigrationItem,
    ): ItemStatus = withContext(Dispatchers.IO) {
        require(item != MigrationItem.Session) { "Legacy archives do not contain sign-in credentials" }
        migration.awaitReady()
        importLegacyPackageItems(migration.storage, source, targetUserId, setOf(item), requireSavedTarget = true)
            .getValue(item)
    }

    internal suspend fun importLegacyPackage(
        storage: MigrationStorage,
        source: PlatformFile,
        targetUserId: Long,
        importBlocks: suspend (LegacyBlocks) -> Boolean = BlockingRepositoryV2::importLegacyBlocks,
    ) {
        importLegacyPackageItems(storage, source, targetUserId, legacyPackageItems, importBlocks = importBlocks)
    }

    internal suspend fun importLegacyPackageItems(
        storage: MigrationStorage,
        source: PlatformFile,
        targetUserId: Long,
        selectedItems: Set<MigrationItem>,
        requireSavedTarget: Boolean = false,
        importBlocks: suspend (LegacyBlocks) -> Boolean = BlockingRepositoryV2::importLegacyBlocks,
    ): Map<MigrationItem, ItemStatus> {
        require(targetUserId > 0)
        return migration.withRecoveryStorage { database, accounts ->
            check(!requireSavedTarget || storage.preferences.sessions.find(targetUserId) != null) {
                "The recovery target account was removed"
            }
            withWorkDirectory { work ->
                val archive = BackupArchive.open(source, work / "legacy.zip", zipUtil, storage.availableBytes)
                val manifest = migrationJson.decodeFromString<LegacyPackageManifest>(
                    archive.requiredText(archiveManifest, 64 * 1024),
                )
                require(manifest.version == 1) { "Unsupported legacy archive format" }
                val mmkv = migrationJson.decodeFromString<LegacyPackagePreferences>(
                    archive.requiredText(archiveMetadata, 16 * 1024 * 1024),
                )
                val attached = (work / archiveDatabase).toKotlinxIoPath().takeIf {
                    manifest.sourceDatabase != null &&
                        (selectedItems.any { item -> item in archiveDatabaseItems } ||
                            MigrationItem.SeriesProgress in selectedItems && mmkv.preferences.sourceUserId == null)
                }
                val originalIds = if (attached != null) {
                    archive.extract("$archiveDirectory/$archiveDatabase", work / archiveDatabase)
                    for (suffix in databaseSuffixes.drop(1)) {
                        val name = "$archiveDirectory/$archiveDatabase$suffix"
                        if (name in archive.entries) archive.extract(name, work / (archiveDatabase + suffix))
                    }
                    legacyDatabaseCounts(attached)
                    legacyAccountUserIds(attached)
                } else {
                    emptySet()
                }
                val originalId = mmkv.preferences.sourceUserId
                    ?: targetUserId.takeIf { it in originalIds }
                    ?: originalIds.singleOrNull()
                val imported = mutableMapOf<MigrationItem, ItemStatus>()
                val failed = mutableSetOf<MigrationItem>()
                suspend fun importItem(item: MigrationItem, restore: suspend () -> Boolean) {
                    if (item !in selectedItems) return
                    try {
                        imported[item] = if (restore()) ItemStatus.Done else ItemStatus.Skipped
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        failed += item
                        logMigrationError(storage, "import ${item.name}", error)
                    }
                }
                val prefs = storage.preferences
                importItem(MigrationItem.Settings) {
                    prefs.importSettings(mmkv.preferences, targetUserId).outcome == LegacyPreferenceOutcome.Done
                }
                importItem(MigrationItem.SearchAndTags) {
                    prefs.importSearchAndTags(mmkv.preferences, targetUserId).outcome == LegacyPreferenceOutcome.Done
                }
                importItem(MigrationItem.SeriesProgress) {
                    val status = prefs.importSeriesProgress(mmkv.preferences, targetUserId, originalId)
                    check(status.outcome != LegacyPreferenceOutcome.NeedsUser) { "Legacy series belongs to multiple accounts" }
                    status.outcome == LegacyPreferenceOutcome.Done
                }
                val global = LegacyGlobalDatabaseMigration(database)
                importItem(MigrationItem.GlobalDatabase) {
                    if (attached == null) false else {
                        global.restoreGlobalTables(attached)
                        true
                    }
                }
                importItem(MigrationItem.TranslationCache) {
                    attached != null && global.restoreTranslations(attached) != null
                }
                for (item in archiveAccountItems) {
                    importItem(item) {
                        require(originalId != null || originalIds.isEmpty()) {
                            "Legacy archive has no unique source account"
                        }
                        attached != null && originalId != null &&
                            LegacyAccountDataSplitter(database, accounts, attached).split(item.tableGroup, targetUserId, originalId)
                    }
                }
                importItem(MigrationItem.LegacyBlocking) { importBlocks(mmkv.blocks) }
                storage.reloadPreferences(prefs.sessions.activeUserId.takeIf { it > 0 })
                check(failed.isEmpty()) { "Could not recover: ${failed.joinToString { it.name }}" }
                imported
            }
        }
    }

    override suspend fun abandonAll() = withContext(Dispatchers.IO) {
        migration.awaitReady()
        migration.abandonAll(migration.storage)
    }

    override fun clearRecoverAfterLogin() {
        migration.storage.preferences.sessions.setRecoverAfterLogin(false)
    }
}

private const val archiveDirectory = "pipixiv-legacy"
private const val archiveManifest = "manifest.json"
private const val archiveMetadata = "legacy.json"
private const val archiveDatabase = "pixiv_db"

private val archiveAccountItems = listOf(MigrationItem.BrowsingHistory, MigrationItem.ReadingProgress, MigrationItem.ReadLater)
private val archiveDatabaseItems = archiveAccountItems + listOf(MigrationItem.GlobalDatabase, MigrationItem.TranslationCache)
private val legacyPackageItems = MigrationItem.entries.filterNot { it == MigrationItem.Session }.toSet()
private val legacyOwnerItems = setOf(MigrationItem.Settings, MigrationItem.SearchAndTags)

@Serializable
private data class LegacyPackageManifest(
    val version: Int = 1,
    val sourceDatabase: String?,
    val counts: Map<String, Long>,
)

@Serializable
private data class LegacyPackagePreferences(
    val preferences: LegacyPreferencesPackage,
    val blocks: LegacyBlocks,
)

private fun BackupArchive.requiredText(name: String, maxBytes: Int): String =
    requireNotNull(read("$archiveDirectory/$name", maxBytes)) { "Missing $name in legacy archive" }
        .decodeToString(throwOnInvalidSequence = true)

private fun checkArchiveExportSpace(
    work: PlatformFile,
    storage: MigrationStorage,
    path: Path,
    legacyPath: Path,
    hasLegacy: Boolean,
) {
    val total = databaseSuffixes.sumOf { suffix ->
        val file = if (hasLegacy) databaseSibling(legacyPath, suffix) else backupSidecar(path, suffix)
        SystemFileSystem.metadataOrNull(file)?.size ?: 0L
    }
    val available = storage.availableBytes(work.toKotlinxIoPath().toString())
    if (available < 50 * 1024 * 1024 || total > (available - 50 * 1024 * 1024) / 2) {
        throw BackupSpaceException()
    }
}

private suspend fun <T> withWorkDirectory(block: suspend (PlatformFile) -> T): T {
    val work = (FileKit.cacheDir / "legacy-recovery-${Uuid.random()}").also { it.createDirectories() }
    try {
        return block(work)
    } finally {
        withContext(NonCancellable) { work.deleteRecursively() }
    }
}
