package com.mrl.pixiv.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.nucleusframework.webview.web.NativeWebView
import dev.nucleusframework.webview.web.WebView
import dev.nucleusframework.webview.web.WebViewNavigator
import dev.nucleusframework.webview.web.WebViewState

internal actual suspend fun checkWebLoginAvailability(): WebLoginAvailability =
    WebLoginAvailability.Available

@Composable
internal actual fun LoginWebView(
    state: WebViewState,
    navigator: WebViewNavigator,
    onError: (Throwable) -> Unit,
    modifier: Modifier,
    onCreated: (NativeWebView) -> Unit,
    content: @Composable () -> Unit,
) {
    WebView(
        state = state,
        navigator = navigator,
        modifier = modifier,
        onCreated = onCreated,
        content = content,
    )
}
