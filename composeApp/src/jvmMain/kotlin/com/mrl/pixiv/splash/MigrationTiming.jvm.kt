package com.mrl.pixiv.splash

import java.io.File

internal actual fun migrationAvailableBytes(path: String): Long = File(path).usableSpace

internal actual fun migrationMemoryBytes(): Long? = with(Runtime.getRuntime()) { totalMemory() - freeMemory() }
