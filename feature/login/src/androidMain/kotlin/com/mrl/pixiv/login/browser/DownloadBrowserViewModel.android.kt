package com.mrl.pixiv.login.browser

actual fun DownloadBrowserViewModel.initKCEF(
    updateState: (DownloadBrowserState.() -> DownloadBrowserState) -> Unit,
    onFinish: () -> Unit
) {
}

actual fun isBrowserAvailable(): Boolean = true
