package com.mrl.pixiv

import coil3.PlatformContext
import coil3.memory.MemoryCache

internal actual fun createPlatformCoilMemoryCache(context: PlatformContext): MemoryCache =
    MemoryCache.Builder()
        .maxSizePercent(context, 0.25)
        .build()
