package com.mrl.pixiv.common.logs

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE

internal actual fun prepareAppLogDirectory(directory: PlatformFile): AutoCloseable {
    val lockPath = Paths.get(directory.absolutePath(), ".writer.lock")
    val channel = FileChannel.open(lockPath, CREATE, WRITE)
    try {
        // One desktop instance owns rotation and trimming when instances share a data directory.
        checkNotNull(channel.tryLock()) { "Application log directory is already in use" }
    } catch (error: Throwable) {
        channel.close()
        throw error
    }
    return AutoCloseable { channel.close() }
}
