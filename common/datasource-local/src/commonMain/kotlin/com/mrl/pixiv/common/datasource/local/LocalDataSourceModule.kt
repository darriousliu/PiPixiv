package com.mrl.pixiv.common.datasource.local

import androidx.room3.RoomDatabase
import androidx.room3.useReaderConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_CREATE
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READWRITE
import androidx.sqlite.driver.bundled.SQLITE_OPEN_URI
import androidx.sqlite.execSQL
import com.mrl.pixiv.common.datasource.local.dao.BlockContentDao
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.dao.NovelTranslationDao
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.toKotlinxIoPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.koin.core.annotation.Single

fun globalDatabasePath(): Path = Path(FileKit.databasesDir.toKotlinxIoPath(), "pixiv_db")

fun accountStorageRoot(): Path = Path(FileKit.filesDir.toKotlinxIoPath(), "accounts")

internal expect fun getGlobalDatabaseBuilder(path: String): RoomDatabase.Builder<PixivDatabase>

internal expect fun getAccountDatabaseBuilder(path: String): RoomDatabase.Builder<AccountDatabase>

@Single
fun provideGlobalDatabaseProvider(): GlobalDatabaseProvider = GlobalDatabaseProvider()

// Resolve the raw database only after startup migration completes.
@Single
fun provideDatabase(provider: GlobalDatabaseProvider): PixivDatabase = provider.requireOpen()

fun buildGlobalDatabase(path: Path = globalDatabasePath()): PixivDatabase {
    path.parent?.let { SystemFileSystem.createDirectories(it) }
    return getGlobalDatabaseBuilder(path.toString())
        .configure()
        .addMigrations(
            PixivDatabase.MIGRATION_1_2,
            PixivDatabase.MIGRATION_2_3,
            PixivDatabase.MIGRATION_3_4,
            PixivDatabase.MIGRATION_4_5,
            PixivDatabase.MIGRATION_5_6,
            PixivDatabase.MIGRATION_6_7,
            PixivDatabase.MIGRATION_7_8,
            PixivDatabase.MIGRATION_8_9,
            PixivDatabase.MIGRATION_9_10,
        )
        .build()
}

fun buildAccountDatabase(path: Path): AccountDatabase {
    path.parent?.let { SystemFileSystem.createDirectories(it) }
    return getAccountDatabaseBuilder(path.toString()).configure().build()
}

private fun <T : RoomDatabase> RoomDatabase.Builder<T>.configure(): RoomDatabase.Builder<T> {
    val driver = BundledSQLiteDriver()
    return setDriver(object : SQLiteDriver by driver {
        // Enable URI parameters on ATTACH so that legacy and backup files are opened read-only.
        override fun open(fileName: String) =
            driver.open(fileName, SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE or SQLITE_OPEN_URI).also { connection ->
                try {
                    connection.execSQL("PRAGMA main.journal_size_limit = $databaseWalSizeLimitBytes")
                } catch (error: Throwable) {
                    connection.close()
                    throw error
                }
            }
    }).setQueryCoroutineContext(Dispatchers.IO)
}

suspend fun PixivDatabase.openForMigration() {
    useReaderConnection { /* Acquiring a connection opens the file and runs Room migrations. */ }
}

@Single(createdAtStart = true)
fun provideAccountDatabaseProvider(): AccountDatabaseProvider = AccountDatabaseProvider()

@Single
fun provideBlockContentDao(provider: GlobalDatabaseProvider): BlockContentDao = StartupBlockContentDao(provider)

@Single
fun provideDownloadDao(provider: GlobalDatabaseProvider): DownloadDao = StartupDownloadDao(provider)

@Single
fun provideNovelTranslationDao(provider: GlobalDatabaseProvider): NovelTranslationDao =
    StartupNovelTranslationDao(provider)
