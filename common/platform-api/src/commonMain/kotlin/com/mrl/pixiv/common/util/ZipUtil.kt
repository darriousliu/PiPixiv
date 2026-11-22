package com.mrl.pixiv.common.util

interface ZipUtil {
    val storesUncompressed: Boolean get() = false

    fun unzip(sourcePath: String, destinationPath: String): Boolean

    fun compress(sourcePath: String, destinationPath: String): Boolean

    fun getZipEntryList(zipFilePath: String): List<Pair<String, Boolean>>

    fun getZipEntryMetadata(zipFilePath: String): List<ZipEntryMetadata>?

    fun getZipEntryContent(
        zipFilePath: String,
        entryName: String,
        maxBytes: Int = Int.MAX_VALUE,
    ): ByteArray?

    fun extractZipEntry(
        zipFilePath: String,
        entryName: String,
        outputPath: String,
        maxBytes: Long = Long.MAX_VALUE,
    ): Boolean
}

class ZipEntryMetadata(val name: String, val size: Long, val isDirectory: Boolean)

class ZipEntryTooLargeException : IllegalArgumentException("ZIP entry exceeds the size limit")

fun createByteArray(size: Int, initializer: (Int) -> Byte): ByteArray = ByteArray(size, initializer)
