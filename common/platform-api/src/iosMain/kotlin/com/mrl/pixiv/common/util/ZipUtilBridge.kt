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

    override fun getZipEntryMetadata(zipFilePath: String): List<ZipEntryMetadata>? =
        error("ZipUtilBridge must be implemented in Swift")

    final override fun getZipEntryContent(zipFilePath: String, entryName: String, maxBytes: Int): ByteArray? {
        if (maxBytes != Int.MAX_VALUE) {
            val entry = getZipEntryMetadata(zipFilePath)?.firstOrNull { it.name == entryName && !it.isDirectory }
                ?: return null
            if (entry.size > maxBytes) throw ZipEntryTooLargeException()
        }
        return readZipEntryContent(zipFilePath, entryName, maxBytes)
    }

    open fun readZipEntryContent(zipFilePath: String, entryName: String, maxBytes: Int): ByteArray? =
        error("ZipUtilBridge must be implemented in Swift")

    override fun extractZipEntry(
        zipFilePath: String,
        entryName: String,
        outputPath: String,
        maxBytes: Long,
    ): Boolean = error("ZipUtilBridge must be implemented in Swift")
}
