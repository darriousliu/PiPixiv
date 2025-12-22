package com.mrl.pixiv.common.util

interface ZipUtil {
    fun unzip(sourcePath: String, destinationPath: String): Boolean

    fun compress(sourcePath: String, destinationPath: String): Boolean

    fun getZipEntryList(zipFilePath: String): List<String>

    fun getZipEntryContent(zipFilePath: String, entryName: String): ByteArray?
}