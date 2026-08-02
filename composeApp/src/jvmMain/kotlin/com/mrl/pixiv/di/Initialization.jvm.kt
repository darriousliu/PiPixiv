package com.mrl.pixiv.di

import com.ctrip.flight.mmkv.MMKVCLibLoader
import com.ctrip.flight.mmkv.MMKVLogLevel
import com.ctrip.flight.mmkv.initialize
import com.mrl.pixiv.common.analytics.logException
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

actual fun initializeMMKV(logLevel: MMKVLogLevel) {
    try {
        initialize(
            rootDir = (FileKit.filesDir / "mmkv").absolutePath(),
            loader = MMKVCLibLoader(::resolveMMKVNativeLibrary),
            logLevel = logLevel,
        )
    } catch (e: Exception) {
        logException(e)
        exitProcess(-1)
    }
}

private fun resolveMMKVNativeLibrary(): String {
    val resourcesDirectory = requireNotNull(
        System.getProperty("compose.application.resources.dir"),
    ) {
        "Compose application resources directory is unavailable. " +
            "Run the desktop app through a Gradle run or packaging task."
    }
    val libraryName = when (val osName = System.getProperty("os.name")) {
        "Mac OS X" -> "libmmkvc.dylib"
        else -> when {
            osName.startsWith("Windows") -> "mmkvc.dll"
            osName.startsWith("Linux") -> "libmmkvc.so"
            else -> error("Unsupported desktop OS: $osName")
        }
    }
    val libraryPath = Path.of(resourcesDirectory, "mmkv", libraryName)
        .toAbsolutePath()
        .normalize()
    check(Files.isRegularFile(libraryPath)) {
        "MMKV native library does not exist: $libraryPath"
    }
    return libraryPath.toRealPath().toString()
}
