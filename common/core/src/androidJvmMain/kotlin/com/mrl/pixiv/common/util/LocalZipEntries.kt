package com.mrl.pixiv.common.util

import co.touchlab.kermit.Logger
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

internal fun localZipEntryMetadata(path: String): List<ZipEntryMetadata>? = readZip(path) { zip ->
    zip.entries().asSequence().map { ZipEntryMetadata(it.name, it.size, it.isDirectory) }.toList()
}

internal fun localZipEntryContent(path: String, name: String, maxBytes: Int): ByteArray? = readZip(path) { zip ->
    val entry = zip.fileEntry(name) ?: return@readZip null
    if (entry.size > maxBytes) throw ZipEntryTooLargeException()
    val bytes = ByteArray(entry.size.toInt())
    zip.getInputStream(entry).use { input ->
        var offset = 0
        input.copyZipEntry(entry.size, entry.crc) { buffer, count ->
            buffer.copyInto(bytes, offset, endIndex = count)
            offset += count
        }
    }
    bytes
}

internal fun extractLocalZipEntry(path: String, name: String, outputPath: String, maxBytes: Long): Boolean = readZip(path) { zip ->
    val entry = zip.fileEntry(name) ?: return@readZip false
    if (entry.size > maxBytes) return@readZip false
    val output = File(outputPath)
    var complete = false
    try {
        zip.getInputStream(entry).use { input ->
            output.outputStream().buffered().use { sink ->
                input.copyZipEntry(entry.size, entry.crc) { buffer, count -> sink.write(buffer, 0, count) }
            }
        }
        complete = true
        true
    } finally {
        if (!complete) output.delete()
    }
} == true

private fun ZipFile.fileEntry(name: String): ZipEntry? = getEntry(name)?.takeUnless { it.isDirectory }?.also {
    if (it.size < 0) throw IOException("Missing ZIP entry size")
}

internal fun InputStream.copyZipEntry(size: Long, expectedCrc: Long, write: (ByteArray, Int) -> Unit) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    val crc = CRC32()
    var remaining = size
    while (true) {
        val readLimit = if (remaining >= buffer.size) buffer.size else remaining.toInt() + 1
        val count = read(buffer, 0, readLimit)
        if (count < 0) break
        if (count > remaining) throw IOException("ZIP entry exceeds the declared size")
        write(buffer, count)
        crc.update(buffer, 0, count)
        remaining -= count
    }
    if (remaining != 0L || crc.value != expectedCrc) throw IOException("Invalid ZIP entry")
}

private fun <T> readZip(path: String, read: (ZipFile) -> T): T? = try {
    ZipFile(path).use(read)
} catch (error: IOException) {
    Logger.e(throwable = error, tag = "ZipUtil") { "Unable to read ZIP archive" }
    null
}
