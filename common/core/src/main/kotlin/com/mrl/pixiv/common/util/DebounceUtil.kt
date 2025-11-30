package com.mrl.pixiv.common.util

object DebounceUtil {
    private const val DEFAULT_DEBOUNCE_TIME = 500L
    private var lastTime = 0L

    fun debounce(
        debounceTime: Long = DEFAULT_DEBOUNCE_TIME,
        action: () -> Unit
    ) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTime > debounceTime) {
            lastTime = currentTime
            action()
        }
    }
}