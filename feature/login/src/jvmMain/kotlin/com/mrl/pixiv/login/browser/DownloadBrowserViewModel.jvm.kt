package com.mrl.pixiv.login.browser

import androidx.lifecycle.viewModelScope
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.browser_initialized
import com.mrl.pixiv.strings.downloading_browser
import com.mrl.pixiv.strings.init_browser
import com.mrl.pixiv.strings.initializing_browser
import com.mrl.pixiv.strings.installing_browser
import com.mrl.pixiv.strings.locating_browser
import com.mrl.pixiv.strings.unknown_error
import com.mrl.pixiv.strings.unzipping_browser
import dev.datlag.kcef.KCEF
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.resolve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val WEBVIEW_INSTALL_DIR = FileKit.filesDir / "webview"

actual fun DownloadBrowserViewModel.initKCEF(
    updateState: (DownloadBrowserState.() -> DownloadBrowserState) -> Unit,
    onFinish: () -> Unit
) {
    updateState {
        copy(progressMessage = AppUtil.getString(RStrings.init_browser))
    }
    viewModelScope.launch(Dispatchers.IO) {
        KCEF.init(
            builder = {
                installDir(WEBVIEW_INSTALL_DIR.file)
                settings {
                    logFile = FileKit.cacheDir.resolve("log").resolve("webview.log").absolutePath()
                    cachePath = FileKit.cacheDir.resolve("webview").absolutePath()
                }
                progress {
                    onDownloading {
                        updateState {
                            copy(
                                progressMessage = AppUtil.getString(
                                    RStrings.downloading_browser,
                                    "%.2f".format(it)
                                )
                            )
                        }
                    }

                    onExtracting {
                        updateState {
                            copy(
                                progressMessage = AppUtil.getString(RStrings.unzipping_browser)
                            )
                        }
                    }

                    onInitialized {
                        updateState {
                            copy(
                                progressMessage = AppUtil.getString(RStrings.browser_initialized)
                            )
                        }
                        onFinish()
                    }

                    onInitializing {
                        updateState {
                            copy(
                                progressMessage = AppUtil.getString(RStrings.initializing_browser)
                            )
                        }
                    }

                    onInstall {
                        updateState {
                            copy(
                                progressMessage = AppUtil.getString(RStrings.installing_browser)
                            )
                        }
                    }
                    onLocating {
                        updateState {
                            copy(
                                progressMessage = AppUtil.getString(RStrings.locating_browser)
                            )
                        }
                    }
                }
            },
            onError = {
                val error = it?.localizedMessage ?: AppUtil.getString(RStrings.unknown_error)
                updateState {
                    copy(progressMessage = error)
                }
            },
        )
    }
}

actual fun isBrowserAvailable(): Boolean {
    return WEBVIEW_INSTALL_DIR.file.exists()
}
