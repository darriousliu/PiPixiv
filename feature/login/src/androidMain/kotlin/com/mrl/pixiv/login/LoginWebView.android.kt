package com.mrl.pixiv.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import dev.nucleusframework.webview.web.NativeWebView
import dev.nucleusframework.webview.web.WebView
import dev.nucleusframework.webview.web.WebViewNavigator
import dev.nucleusframework.webview.web.WebViewState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import kotlin.coroutines.resume

// ProxyController applies one override to the entire process. Keep it until this WebView is closed.
private val loginWebViewProxy = Mutex()
private val proxyCallbackExecutor = Executor { it.run() }

internal actual suspend fun checkWebLoginAvailability(): WebLoginAvailability =
    WebLoginAvailability.Available

@Composable
internal actual fun LoginWebView(
    state: WebViewState,
    navigator: WebViewNavigator,
    bypassSetting: BypassSetting,
    onError: (Throwable) -> Unit,
    modifier: Modifier,
    onCreated: (NativeWebView) -> Unit,
    content: @Composable () -> Unit,
) {
    val currentOnError by rememberUpdatedState(onError)
    var routeReady by remember(state, bypassSetting) { mutableStateOf(false) }
    val released = remember(state, bypassSetting) { CompletableDeferred<Unit>() }
    LaunchedEffect(state, bypassSetting) {
        loginWebViewProxy.withLock {
            var controller: ProxyController? = null
            try {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                    val proxyController = ProxyController.getInstance()
                    controller = proxyController
                    withContext(NonCancellable) { proxyController.applyRoute(bypassSetting) }
                } else if (bypassSetting != BypassSetting.System) {
                    currentOnError(WebLoginRouteUnavailableException())
                    return@withLock
                }
                currentCoroutineContext().ensureActive()
                routeReady = true
                awaitCancellation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: RuntimeException) {
                currentOnError(WebLoginRouteUnavailableException())
            } finally {
                val proxyController = controller
                withContext(NonCancellable) {
                    if (routeReady && state.webView != null) released.await()
                    if (proxyController != null && bypassSetting != BypassSetting.System) {
                        proxyController.restoreSystemRoute()
                    }
                }
            }
        }
    }

    if (!routeReady) return
    WebView(
        state = state,
        navigator = navigator,
        modifier = modifier,
        onCreated = onCreated,
        onDispose = { released.complete(Unit) },
        content = content,
    )
}

private suspend fun ProxyController.restoreSystemRoute() {
    try {
        applyRoute(BypassSetting.System)
    } catch (_: RuntimeException) {
        Logger.w(tag = "LoginWebView") { "Could not clear the WebView proxy override" }
    }
}

private suspend fun ProxyController.applyRoute(setting: BypassSetting) {
    suspendCancellableCoroutine { continuation ->
        val done = Runnable { continuation.resume(Unit) }
        when (setting) {
            BypassSetting.System -> clearProxyOverride(proxyCallbackExecutor, done)
            BypassSetting.Direct, is BypassSetting.ECH -> setProxyOverride(
                ProxyConfig.Builder().addDirect().build(),
                proxyCallbackExecutor,
                done,
            )
            is BypassSetting.Proxy -> {
                val scheme = when (setting.proxyType) {
                    BypassSetting.Proxy.ProxyType.HTTP -> "http"
                    BypassSetting.Proxy.ProxyType.SOCKS -> "socks5"
                }
                val host = setting.host.removeSurrounding("[", "]")
                val proxyHost = if (':' in host) "[$host]" else host
                val config = ProxyConfig.Builder()
                    .addProxyRule("$scheme://$proxyHost:${setting.port}")
                    .removeImplicitRules()
                    .build()
                setProxyOverride(config, proxyCallbackExecutor, done)
            }
        }
    }
}
