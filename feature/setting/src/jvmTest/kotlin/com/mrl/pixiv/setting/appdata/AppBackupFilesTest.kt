package com.mrl.pixiv.setting.appdata

import com.mrl.pixiv.common.datasource.local.AccountHandle
import com.mrl.pixiv.common.datasource.local.buildAccountDatabase
import com.mrl.pixiv.common.datasource.local.buildGlobalDatabase
import com.mrl.pixiv.common.util.BackupSpaceException
import com.mrl.pixiv.common.util.JvmZipUtil
import com.mrl.pixiv.common.util.ZipEntryTooLargeException
import io.github.vinceglb.filekit.PlatformFile
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AppBackupFilesTest {
    @Test
    fun insufficientSpaceStopsExportAndImportBeforeStagingFiles() = runBlocking {
        val root = Files.createTempDirectory("app-backup-space").toFile()
        val globalPath = root.resolve("global.db").absolutePath
        val accountPath = root.resolve("account.db").absolutePath
        val global = buildGlobalDatabase(Path(globalPath))
        val account = buildAccountDatabase(Path(accountPath))
        try {
            val workDir = root.resolve("work").apply { mkdir() }
            val destination = root.resolve("export.zip")
            val backup = AppBackupFiles(JvmZipUtil()) { 0 }
            assertFailsWith<BackupSpaceException> {
                backup.exportV4(
                    metadata = "{}",
                    global = global,
                    account = AccountHandle(1, account),
                    globalDatabasePath = globalPath,
                    accountDatabasePath = accountPath,
                    workDir = PlatformFile(workDir),
                    destination = PlatformFile(destination),
                )
            }
            assertFalse(destination.exists())

            val source = createV4Archive(root, dataSize = 100)
            val enoughForZipOnly = AppBackupFiles(JvmZipUtil()) { 50L * 1024 * 1024 + source.length() }
            assertFailsWith<BackupSpaceException> {
                enoughForZipOnly.read(PlatformFile(source), PlatformFile(workDir))
            }
            assertTrue(workDir.list().orEmpty().isEmpty())
        } finally {
            global.close()
            account.close()
            root.deleteRecursively()
        }
        Unit
    }

    @Test
    fun v4RejectsOversizedMetadataBeforeReadingIt() = runBlocking {
        val root = Files.createTempDirectory("app-backup-size").toFile()
        try {
            val workDir = root.resolve("work").apply { mkdir() }
            val source = createV4Archive(root, dataSize = 16L * 1024 * 1024 + 1)
            val zip = JvmZipUtil()
            assertFailsWith<ZipEntryTooLargeException> {
                zip.getZipEntryContent(source.absolutePath, "pipixiv-backup/data.json", maxBytes = 16 * 1024 * 1024)
            }
            assertFailsWith<ZipEntryTooLargeException> {
                AppBackupFiles(zip) { Long.MAX_VALUE }.read(PlatformFile(source), PlatformFile(workDir))
            }
        } finally {
            root.deleteRecursively()
        }
        Unit
    }

    private fun createV4Archive(root: File, dataSize: Long): File {
        val source = root.resolve("pipixiv-backup").apply { mkdir() }
        RandomAccessFile(source.resolve("data.json"), "rw").use { it.setLength(dataSize) }
        return root.resolve("import.zip").also {
            assertTrue(JvmZipUtil().compress(source.absolutePath, it.absolutePath))
        }
    }
}
