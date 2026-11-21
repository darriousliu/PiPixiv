package com.mrl.pixiv.common.util

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.parent
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.source
import io.github.vinceglb.filekit.startAccessingSecurityScopedResource
import io.github.vinceglb.filekit.stopAccessingSecurityScopedResource
import io.github.vinceglb.filekit.toKotlinxIoPath
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class BackupSpaceException : IllegalStateException("Insufficient storage for the backup operation")

class BackupArchive private constructor(
    private val zipUtil: ZipUtil,
    private val path: String,
    val entries: Map<String, Long>,
) {
    fun read(name: String, maxBytes: Int): ByteArray? {
        val size = entries[name] ?: return null
        if (size > maxBytes) throw ZipEntryTooLargeException()
        return checkNotNull(zipUtil.getZipEntryContent(path, name, size.toInt())) {
            "Invalid backup entry: $name"
        }
    }

    fun extract(name: String, target: PlatformFile) {
        val size = requireNotNull(entries[name]) { "Missing backup entry: $name" }
        check(zipUtil.extractZipEntry(path, name, target.absolutePath(), size)) {
            "Invalid backup entry: $name"
        }
    }

    companion object {
        suspend fun open(
            source: PlatformFile,
            stagingFile: PlatformFile,
            zipUtil: ZipUtil,
            availableBytes: (String) -> Long = { availableStorageBytes(it) },
            timing: BackupTiming? = null,
        ): BackupArchive {
            val cachePath = checkNotNull(stagingFile.parent()).toKotlinxIoPath().toString()
            val compressedSize = source.size()
            val available = availableBytes(cachePath)
            requireBackupSpace(available, importBudget(compressedSize.coerceAtLeast(0), 0))

            // Android document providers may expose only a stream. Stage it with a hard limit
            // before inspecting its directory. Seekable files get the full check before copying.
            val originalEntries = if (!source.absolutePath().startsWith("content://")) {
                val accessGranted = source.startAccessingSecurityScopedResource()
                try {
                    timing.measureBackupStep(BackupStep.ReadZipDirectory) {
                        zipUtil.readDirectory(source.toKotlinxIoPath().toString())
                    }.also { directory ->
                        requireBackupSpace(available, importBudget(compressedSize.coerceAtLeast(0), directory.totalSize))
                    }
                } finally {
                    if (accessGranted) source.stopAccessingSecurityScopedResource()
                }
            } else {
                null
            }
            val stagingBudget = available - backupDiskMargin
            val copied = timing.measureBackupStep(BackupStep.CopyToCache) {
                source.copyForBackup(stagingFile, stagingBudget)
            }
            val path = stagingFile.toKotlinxIoPath().toString()
            val directory = timing.measureBackupStep(BackupStep.ReadZipDirectory) {
                zipUtil.readDirectory(path)
            }
            if (originalEntries != null) {
                check(originalEntries == directory && (compressedSize < 0 || compressedSize == copied)) {
                    "Backup archive changed during import"
                }
            }
            requireBackupSpace(available, importBudget(copied, directory.totalSize))
            requireBackupSpace(availableBytes(cachePath), importBudget(0, directory.totalSize))
            return BackupArchive(zipUtil, path, directory.files)
        }
    }
}

fun checkBackupExportSpace(
    cacheDirectory: PlatformFile,
    databasePaths: List<String>,
    availableBytes: (String) -> Long = { availableStorageBytes(it) },
) {
    val databaseSize = databasePaths.fold(0L) { total, path ->
        listOf(path, "$path-wal").fold(total) { size, file ->
            checkedAdd(size, SystemFileSystem.metadataOrNull(Path(file))?.size ?: 0)
        }
    }
    val halfSize = databaseSize / 2 + databaseSize % 2
    val required = checkedAdd(checkedAdd(checkedAdd(databaseSize, databaseSize), halfSize), backupDiskMargin)
    requireBackupSpace(availableBytes(cacheDirectory.toKotlinxIoPath().toString()), required)
}

private const val backupDiskMargin = 50L * 1024 * 1024

private data class BackupDirectory(val files: Map<String, Long>, val totalSize: Long)

private fun ZipUtil.readDirectory(path: String): BackupDirectory {
    val metadata = requireNotNull(getZipEntryMetadata(path)) { "Invalid ZIP archive" }
    val files = mutableMapOf<String, Long>()
    var totalSize = 0L
    for (entry in metadata) {
        require(entry.size >= 0) { "Missing size in ZIP directory" }
        totalSize = checkedAdd(totalSize, entry.size)
        if (!entry.isDirectory) {
            require(files.put(entry.name, entry.size) == null) { "Duplicate ZIP entry" }
        }
    }
    return BackupDirectory(files, totalSize)
}

private suspend fun PlatformFile.copyForBackup(destination: PlatformFile, limit: Long): Long {
    val buffer = Buffer()
    var copied = 0L
    source().use { input ->
        destination.sink().use { output ->
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.readAtMostTo(buffer, 64 * 1024L)
                if (count == -1L) break
                if (count > limit - copied) throw BackupSpaceException()
                output.write(buffer, count)
                copied += count
            }
        }
    }
    return copied
}

private fun importBudget(compressedSize: Long, uncompressedSize: Long): Long {
    val decompressionBudget = checkedAdd(checkedAdd(uncompressedSize, uncompressedSize), uncompressedSize)
    return checkedAdd(checkedAdd(compressedSize, decompressionBudget), backupDiskMargin)
}

private fun checkedAdd(a: Long, b: Long): Long {
    if (b > Long.MAX_VALUE - a) throw BackupSpaceException()
    return a + b
}

private fun requireBackupSpace(available: Long, required: Long) {
    if (available < required) throw BackupSpaceException()
}
