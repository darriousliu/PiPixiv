package com.mrl.pixiv.common.util

/**
 * Constructible Kotlin superclass for the Swift implementation of [ZipUtil].
 */
open class ZipUtilBridge : ZipUtil {
    override fun unzip(sourcePath: String, destinationPath: String): Boolean =
        error("ZipUtilBridge must be implemented in Swift")

    override fun compress(sourcePath: String, destinationPath: String): Boolean =
        error("ZipUtilBridge must be implemented in Swift")

    override fun getZipEntryList(zipFilePath: String): List<Pair<String, Boolean>> =
        error("ZipUtilBridge must be implemented in Swift")

    override fun getZipEntryContent(zipFilePath: String, entryName: String): ByteArray? =
        error("ZipUtilBridge must be implemented in Swift")
}