package com.mrl.pixiv.common.mmkv

import platform.Foundation.NSRecursiveLock

private val accountStorageLock = NSRecursiveLock()

actual fun <T> withAccountStorageLock(block: () -> T): T {
    accountStorageLock.lock()
    return try {
        block()
    } finally {
        accountStorageLock.unlock()
    }
}
