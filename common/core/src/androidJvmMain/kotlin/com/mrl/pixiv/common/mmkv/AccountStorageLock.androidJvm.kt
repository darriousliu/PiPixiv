package com.mrl.pixiv.common.mmkv

private val accountStorageLock = Any()

actual fun <T> withAccountStorageLock(block: () -> T): T =
    synchronized(accountStorageLock, block)
