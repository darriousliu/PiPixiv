package com.mrl.pixiv.splash

import android.os.Debug

internal actual fun migrationMemorySample(): MigrationMemorySample? =
    MigrationMemorySample(Debug.getPss() * 1024)
