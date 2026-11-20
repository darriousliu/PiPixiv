package com.mrl.pixiv.splash

import android.os.Debug
import java.io.File

internal actual fun migrationAvailableBytes(path: String): Long = File(path).usableSpace

internal actual fun migrationMemoryBytes(): Long? = Debug.getPss() * 1024
