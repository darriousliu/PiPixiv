package com.mrl.pixiv.splash


internal actual fun migrationMemoryBytes(): Long? = with(Runtime.getRuntime()) { totalMemory() - freeMemory() }
