package com.mrl.pixiv.login.browser

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.login.isKCEFInitialized
import com.mrl.pixiv.strings.browser_initialized
import com.mrl.pixiv.strings.downloading_browser
import com.mrl.pixiv.strings.init_browser
import com.mrl.pixiv.strings.initializing_browser
import com.mrl.pixiv.strings.installing_browser
import com.mrl.pixiv.strings.locating_browser
import com.mrl.pixiv.strings.unknown_error
import com.mrl.pixiv.strings.unzipping_browser
import net.sergeych.sprintf.format
import org.koin.android.annotation.KoinViewModel

@Stable
data class DownloadBrowserState(
    val isLoading: Boolean = false,
    val progressMessage: String = "",

    )

sealed interface DownloadBrowserEffect : SideEffect {
    data object NavigateToLogin : DownloadBrowserEffect
}

@KoinViewModel
class DownloadBrowserViewModel : BaseMviViewModel<DownloadBrowserState, ViewIntent>(
    initialState = DownloadBrowserState()
) {
    override suspend fun handleIntent(intent: ViewIntent) {

    }

    fun initKCEF() {
        launchIO {
            initKCEF(
                onInit = {
                    updateState {
                        copy(progressMessage = AppUtil.getString(RStrings.init_browser))
                    }
                },
                onDownloading = {
                    updateState {
                        copy(
                            progressMessage = AppUtil.getString(
                                RStrings.downloading_browser,
                                "%.2f".format(it)
                            )
                        )
                    }
                },
                onExtracting = {
                    updateState {
                        copy(
                            progressMessage = AppUtil.getString(RStrings.unzipping_browser)
                        )
                    }
                },
                onInitialized = {
                    updateState {
                        copy(
                            progressMessage = AppUtil.getString(RStrings.browser_initialized)
                        )
                    }
                    isKCEFInitialized = true
                    sendEffect(DownloadBrowserEffect.NavigateToLogin)
                },
                onInitializing = {
                    updateState {
                        copy(
                            progressMessage = AppUtil.getString(RStrings.initializing_browser)
                        )
                    }
                },
                onInstall = {
                    updateState {
                        copy(
                            progressMessage = AppUtil.getString(RStrings.installing_browser)
                        )
                    }
                },
                onLocating = {
                    updateState {
                        copy(
                            progressMessage = AppUtil.getString(RStrings.locating_browser)
                        )
                    }
                },
                onError = {
                    val error = it?.message ?: AppUtil.getString(RStrings.unknown_error)
                    updateState {
                        copy(progressMessage = error)
                    }
                }
            )
        }
    }
}

expect suspend fun initKCEF(
    onInit: () -> Unit = {},
    onDownloading: (Float) -> Unit = {},
    onExtracting: () -> Unit = {},
    onInitialized: () -> Unit = {},
    onInitializing: () -> Unit = {},
    onInstall: () -> Unit = {},
    onLocating: () -> Unit = {},
    onError: (Throwable?) -> Unit = {},
)

expect fun isBrowserAvailable(): Boolean
