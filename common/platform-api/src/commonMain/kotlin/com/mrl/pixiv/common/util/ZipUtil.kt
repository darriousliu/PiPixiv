package com.mrl.pixiv.common.util

interface ZipUtil {
    fun unzip(sourcePath: String, destinationPath: String): Boolean

    fun compress(sourcePath: String, destinationPath: String): Boolean

    fun getZipEntryList(zipFilePath: String): List<Pair<String, Boolean>>

    fun getZipEntryContent(zipFilePath: String, entryName: String): ByteArray?

    fun extractZipEntry(zipFilePath: String, entryName: String, outputPath: String): Boolean
}

fun createByteArray(size: Int, initializer: (Int) -> Byte): ByteArray = ByteArray(size, initializer)
