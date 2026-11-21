package com.mrl.pixiv.splash


internal actual fun migrationMemorySample(): MigrationMemorySample? = with(Runtime.getRuntime()) {
    MigrationMemorySample(totalMemory() - freeMemory())
}
