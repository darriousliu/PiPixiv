package com.mrl.pixiv.login.browser

actual suspend fun initKCEF(
    onInit: () -> Unit,
    onDownloading: (Float) -> Unit,
    onExtracting: () -> Unit,
    onInitialized: () -> Unit,
    onInitializing: () -> Unit,
    onInstall: () -> Unit,
    onLocating: () -> Unit,
    onError: (Throwable?) -> Unit
) {
}

actual fun isBrowserAvailable(): Boolean = true
