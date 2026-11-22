package com.mrl.pixiv.common.util

import io.github.vinceglb.filekit.PlatformFile
import java.io.RandomAccessFile
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BackupExportSpaceTest {
    @Test
    fun uncompressedExportReservesSpaceForTheSameVolumeDestination() = withDatabaseFiles { cache, paths, databaseBytes ->
        val uncompressedZip = object : ZipUtil by JvmZipUtil() {
            override val storesUncompressed: Boolean get() = true
        }
        val requiredBytes = 3 * databaseBytes + diskMargin

        assertFailsWith<BackupSpaceException> {
            checkBackupExportSpace(cache, paths, uncompressedZip) { requiredBytes - 1 }
        }
        checkBackupExportSpace(cache, paths, uncompressedZip) { requiredBytes }
    }

    @Test
    fun compressedExportKeepsTheExistingSpaceBudget() = withDatabaseFiles { cache, paths, databaseBytes ->
        val requiredBytes = 2 * databaseBytes + (databaseBytes + 1) / 2 + diskMargin
        val compressedZip = JvmZipUtil()

        assertFailsWith<BackupSpaceException> {
            checkBackupExportSpace(cache, paths, compressedZip) { requiredBytes - 1 }
        }
        checkBackupExportSpace(cache, paths, compressedZip) { requiredBytes }
    }

    private fun withDatabaseFiles(verify: (PlatformFile, List<String>, Long) -> Unit) {
        val cache = createTempDirectory("backup-export-space-").toFile()
        try {
            val global = cache.resolve("pixiv_db")
            val account = cache.resolve("account.db")
            val globalWal = cache.resolve("pixiv_db-wal")
            RandomAccessFile(global, "rw").use { it.setLength(128L * 1024 * 1024) }
            account.writeBytes(byteArrayOf(0))
            globalWal.writeBytes(byteArrayOf(0, 0))
            verify(
                PlatformFile(cache),
                listOf(global.path, account.path),
                global.length() + account.length() + globalWal.length(),
            )
        } finally {
            cache.deleteRecursively()
        }
    }

    private companion object {
        const val diskMargin = 50L * 1024 * 1024
    }
}
