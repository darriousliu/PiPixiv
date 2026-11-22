package com.mrl.pixiv.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import dev.nucleusframework.webview.web.NativeWebView
import dev.nucleusframework.webview.web.WebViewNavigator
import dev.nucleusframework.webview.web.WebViewState

internal enum class WebLoginAvailability {
    Available,
    Unavailable,
}

internal expect suspend fun checkWebLoginAvailability(): WebLoginAvailability

internal class WebLoginRouteUnavailableException :
    IllegalStateException("Web sign-in cannot apply the selected network route")

@Composable
internal expect fun LoginWebView(
    state: WebViewState,
    navigator: WebViewNavigator,
    bypassSetting: BypassSetting,
    onError: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
    onCreated: (NativeWebView) -> Unit = {},
    content: @Composable () -> Unit = {},
)
