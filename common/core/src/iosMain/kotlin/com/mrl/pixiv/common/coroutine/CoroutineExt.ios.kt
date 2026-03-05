package com.mrl.pixiv.common.coroutine

import kotlinx.coroutines.CoroutineScope

actual val ProcessLifecycleScope: CoroutineScope
    get() = mainScope