package com.mrl.pixiv.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import dev.nucleusframework.webview.web.ActualWebView
import dev.nucleusframework.webview.web.NativeWebView
import dev.nucleusframework.webview.web.WebView
import dev.nucleusframework.webview.web.WebViewNavigator
import dev.nucleusframework.webview.web.WebViewState
import dev.nucleusframework.webview.web.defaultWebViewFactory
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Network.nw_endpoint_create_host
import platform.Network.nw_proxy_config_create_http_connect
import platform.Network.nw_proxy_config_create_socksv5
import platform.Network.nw_proxy_config_set_failover_allowed
import platform.WebKit.WKWebsiteDataStore

internal actual suspend fun checkWebLoginAvailability(): WebLoginAvailability =
    WebLoginAvailability.Available

@Composable
@OptIn(ExperimentalForeignApi::class)
internal actual fun LoginWebView(
    state: WebViewState,
    navigator: WebViewNavigator,
    bypassSetting: BypassSetting,
    onError: (Throwable) -> Unit,
    modifier: Modifier,
    onCreated: (NativeWebView) -> Unit,
    content: @Composable () -> Unit,
) {
    if (bypassSetting == BypassSetting.System) {
        WebView(
            state = state,
            navigator = navigator,
            modifier = modifier,
            onCreated = onCreated,
            content = content,
        )
        return
    }

    val store = remember(state, bypassSetting) {
        // An empty WKWebsiteDataStore proxy list clears the override and uses the system route.
        (bypassSetting as? BypassSetting.Proxy)?.createProxyDataStore()
    }
    if (store == null) {
        val currentOnError by rememberUpdatedState(onError)
        LaunchedEffect(state, bypassSetting) {
            currentOnError(WebLoginRouteUnavailableException())
        }
        return
    }

    // Login only requests the initial page and lets WKWebView handle intercepted redirects.
    state.webView?.let { webView ->
        LaunchedEffect(webView, state) {
            snapshotFlow { state.content }.collect(webView::loadContent)
        }
    }
    ActualWebView(
        state = state,
        navigator = navigator,
        modifier = modifier,
        onCreated = onCreated,
        onDispose = { it.stopLoading() },
        factory = { param ->
            param.config.websiteDataStore = store
            defaultWebViewFactory(param)
        },
        content = content,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun BypassSetting.Proxy.createProxyDataStore(): WKWebsiteDataStore? {
    val endpoint = nw_endpoint_create_host(host.removeSurrounding("[", "]"), port.toString())
        ?: return null
    val proxy = when (proxyType) {
        BypassSetting.Proxy.ProxyType.HTTP -> nw_proxy_config_create_http_connect(endpoint, null)
        BypassSetting.Proxy.ProxyType.SOCKS -> nw_proxy_config_create_socksv5(endpoint)
    } ?: return null
    nw_proxy_config_set_failover_allowed(proxy, false)
    return WKWebsiteDataStore.nonPersistentDataStore().apply {
        proxyConfigurations = listOf(proxy)
    }
}
