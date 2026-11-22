package com.mrl.pixiv.common.mmkv

expect fun <T> withAccountStorageLock(block: () -> T): T
