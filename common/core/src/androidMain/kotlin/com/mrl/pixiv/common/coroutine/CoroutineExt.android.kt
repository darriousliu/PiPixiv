package com.mrl.pixiv.common.coroutine

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope

actual val ProcessLifecycleScope: CoroutineScope
    get() = ProcessLifecycleOwner.get().lifecycleScope