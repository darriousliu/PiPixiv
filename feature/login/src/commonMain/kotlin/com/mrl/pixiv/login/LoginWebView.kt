package com.mrl.pixiv.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.nucleusframework.webview.web.NativeWebView
import dev.nucleusframework.webview.web.WebViewNavigator
import dev.nucleusframework.webview.web.WebViewState

internal enum class WebLoginAvailability {
    Available,
    Unavailable,
}

internal expect suspend fun checkWebLoginAvailability(): WebLoginAvailability

@Composable
internal expect fun LoginWebView(
    state: WebViewState,
    navigator: WebViewNavigator,
    onError: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
    onCreated: (NativeWebView) -> Unit = {},
    content: @Composable () -> Unit = {},
)
