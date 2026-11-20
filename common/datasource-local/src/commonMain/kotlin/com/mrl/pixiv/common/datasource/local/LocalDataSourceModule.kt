package com.mrl.pixiv.common.datasource.local

import androidx.room3.RoomDatabase
import androidx.room3.useReaderConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
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
fun provideDatabase(): PixivDatabase = buildGlobalDatabase()

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

private fun <T : RoomDatabase> RoomDatabase.Builder<T>.configure(): RoomDatabase.Builder<T> =
    setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO)

suspend fun PixivDatabase.openForMigration() {
    useReaderConnection { /* Acquiring a connection opens the file and runs Room migrations. */ }
}

@Single(createdAtStart = true)
fun provideAccountDatabaseProvider(): AccountDatabaseProvider = AccountDatabaseProvider()

@Single
fun provideBlockContentDao(database: PixivDatabase): BlockContentDao = database.blockContentDao()

@Single
fun provideDownloadDao(database: PixivDatabase): DownloadDao = database.downloadDao()

@Single
fun provideNovelTranslationDao(database: PixivDatabase): NovelTranslationDao =
    database.novelTranslationDao()
