package com.mrl.pixiv.splash

import com.mrl.pixiv.common.data.setting.AccountPreference
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.data.setting.toUserPreference
import com.mrl.pixiv.common.datasource.local.AccountDatabaseProvider
import com.mrl.pixiv.common.datasource.local.GlobalDatabaseProvider
import com.mrl.pixiv.common.logs.AppLogStore
import com.mrl.pixiv.common.mmkv.AccountMMKVOwner
import com.mrl.pixiv.common.mmkv.mmkvOwner
import com.mrl.pixiv.common.repository.LegacyAccountPreferencesMigration
import com.mrl.pixiv.common.repository.readAccountPreference
import com.mrl.pixiv.common.repository.readDevicePreference
import com.mrl.pixiv.common.util.JvmZipUtil
import kotlinx.io.files.Path
import java.io.File

internal class MigrationTestRun(
    val path: File,
    debug: Boolean = true,
    private val migrateBlocks: suspend () -> Boolean = { false },
) {
    val files = path.resolve("files").apply { mkdirs() }
    private val accountsRoot = Path(files.resolve("accounts").absolutePath)
    val databasePath = Path(path.resolve("pixiv_db").absolutePath)
    val accounts = AccountDatabaseProvider { accountsRoot }
    val database = GlobalDatabaseProvider { databasePath }
    val migration = StartupMigration(database, accounts)
    val recovery = StartupDataRecoveryRepository(migration, JvmZipUtil())
    private val mmkv = path.resolve("mmkv").apply { mkdirs() }.absolutePath
    val app = mmkvOwner("pixiv.app", mmkv)
    val legacy = mmkvOwner("pixiv.user", mmkv)
    private val openedAccounts = mutableMapOf<Long, AccountMMKVOwner>()
    val preferences = LegacyAccountPreferencesMigration(app, legacy, ::accountPreferences)
    var reloaded: UserPreference? = null
        private set
    var observedBlocks = false
        private set
    val storage = MigrationStorage(
        marker = app,
        legacy = legacy,
        databasePath = { databasePath },
        accountRoot = { accountsRoot },
        filesRoot = { Path(files.absolutePath) },
        preferences = preferences,
        reloadPreferences = { uid ->
            val accountPreference = uid?.let { readAccountPreference(accountPreferences(it)) } ?: AccountPreference()
            reloaded = readDevicePreference(app, legacy).toUserPreference(accountPreference)
        },
        observeBlocks = { observedBlocks = true },
        migrateBlocks = {
            check(observedBlocks) { "Must observe global blocks before attempting legacy blocks" }
            migrateBlocks()
        },
        debug = debug,
    )

    fun accountPreferences(uid: Long) = openedAccounts.getOrPut(uid) { AccountMMKVOwner(uid, files.absolutePath) }

    fun debug(config: MigrationDebug?) {
        val file = files.resolve("migration-debug.json")
        if (config == null) file.delete() else file.writeText(migrationJson.encodeToString(config))
    }

    suspend fun start(retry: Boolean = false) = migration.run(storage, retry)

    suspend fun <T> useRun(block: suspend (MigrationTestRun) -> T): T = try {
        block(this)
    } finally {
        accounts.close()
        database.close()
        openedAccounts.values.forEach { it.kv.close() }
        legacy.kv.close()
        app.kv.close()
    }
}

internal fun lastMigrationTiming(): MigrationTiming {
    val message = checkNotNull(AppLogStore.state.value.current.lastOrNull {
        it.message.startsWith(migrationTimingPrefix)
    }).message
    return migrationJson.decodeFromString(message.removePrefix(migrationTimingPrefix))
}

internal fun prepareMigrationRun(root: File, fixture: File, name: String): File =
    root.resolve(name).also { path ->
        check(fixture.copyRecursively(path))
        path.resolve("pixiv_db-wal").createNewFile()
        path.resolve("pixiv_db-shm").createNewFile()
    }
