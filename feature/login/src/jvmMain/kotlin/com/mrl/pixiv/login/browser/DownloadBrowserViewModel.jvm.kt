package com.mrl.pixiv.login.browser

import dev.datlag.kcef.KCEF
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.resolve

private val WEBVIEW_INSTALL_DIR = FileKit.filesDir / "webview"

actual suspend fun initKCEF(
    onInit: () -> Unit,
    onDownloading: (Float) -> Unit,
    onExtracting: () -> Unit,
    onInitialized: () -> Unit,
    onInitializing: () -> Unit,
    onInstall: () -> Unit,
    onLocating: () -> Unit,
    onError: (Throwable?) -> Unit,
) {
    onInit()
    KCEF.init(
        builder = {
            installDir(WEBVIEW_INSTALL_DIR.file)
            settings {
                logFile = FileKit.cacheDir.resolve("log").resolve("webview.log").absolutePath()
                cachePath = FileKit.cacheDir.resolve("webview").absolutePath()
            }
            progress {
                onDownloading(onDownloading)

                onExtracting(onExtracting)

                onInitialized(onInitialized)

                onInitializing(onInitializing)

                onInstall(onInstall)
                onLocating(onLocating)
            }
        },
        onError = onError,
    )
}

actual fun isBrowserAvailable(): Boolean {
    return WEBVIEW_INSTALL_DIR.file.exists()
}
