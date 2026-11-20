package com.mrl.pixiv.splash

import android.os.Debug

internal actual fun migrationMemoryBytes(): Long? = Debug.getPss() * 1024
