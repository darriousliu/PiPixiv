package com.mrl.pixiv.login.browser

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.common.viewmodel.ViewIntent
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
        initKCEF(
            updateState = ::updateState,
            onFinish = {
                sendEffect(DownloadBrowserEffect.NavigateToLogin)
            }
        )
    }
}

expect fun DownloadBrowserViewModel.initKCEF(
    updateState: (DownloadBrowserState.() -> DownloadBrowserState) -> Unit,
    onFinish: () -> Unit
)

expect fun isBrowserAvailable(): Boolean
