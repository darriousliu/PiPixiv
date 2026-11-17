package com.mrl.pixiv.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import dev.nucleusframework.webview.web.ActualWebView
import dev.nucleusframework.webview.web.NativeWebView
import dev.nucleusframework.webview.web.WebView
import dev.nucleusframework.webview.web.WebViewFactoryParam
import dev.nucleusframework.webview.web.WebViewNavigator
import dev.nucleusframework.webview.web.WebViewState
import dev.nucleusframework.webview.web.defaultWebViewFactory
import dev.nucleusframework.webview.web.windows.WindowsWebView2NativeWebView
import kotlinx.coroutines.CancellationException

internal class WebViewRuntimeUnavailableException :
    IllegalStateException("Microsoft Edge WebView2 Runtime is unavailable")

/** A failed factory has no native handle and must never be treated as a Windows native view. */
internal class FailedLoginNativeWebView(val failure: Throwable) : NativeWebView()

internal fun createLoginNativeWebView(
    availability: WebLoginAvailability,
    factory: () -> NativeWebView,
): NativeWebView {
    if (availability == WebLoginAvailability.Unavailable) {
        return FailedLoginNativeWebView(WebViewRuntimeUnavailableException())
    }
    return try {
        factory()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        FailedLoginNativeWebView(error)
    } catch (error: LinkageError) {
        FailedLoginNativeWebView(error)
    }
}

private inline fun recoverLoginWebViewFailure(onError: (Throwable) -> Unit, operation: () -> Unit) {
    try {
        operation()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        onError(error)
    } catch (error: LinkageError) {
        onError(error)
    }
}

@Composable
internal actual fun LoginWebView(
    state: WebViewState,
    navigator: WebViewNavigator,
    onError: (Throwable) -> Unit,
    modifier: Modifier,
    onCreated: (NativeWebView) -> Unit,
    content: @Composable () -> Unit,
) {
    if (!System.getProperty("os.name").startsWith("Windows")) {
        WebView(
            state = state,
            modifier = modifier,
            navigator = navigator,
            onCreated = onCreated,
            content = content,
        )
        return
    }
    var availability by remember(state) { mutableStateOf<WebLoginAvailability?>(null) }
    val currentOnError by rememberUpdatedState(onError)
    val currentOnCreated by rememberUpdatedState(onCreated)
    LaunchedEffect(state) {
        availability = checkWebLoginAvailability()
        if (availability == WebLoginAvailability.Unavailable) {
            currentOnError(WebViewRuntimeUnavailableException())
        }
    }
    if (availability != WebLoginAvailability.Available) return

    val factory: (WebViewFactoryParam) -> NativeWebView = remember(state) {
        { param ->
            createLoginNativeWebView(WebLoginAvailability.Available) {
                val nativeWebView = defaultWebViewFactory(param)
                check(param.parentHwnd == 0L || nativeWebView is WindowsWebView2NativeWebView) {
                    "WebView2 native backend is unavailable"
                }
                nativeWebView
            }
        }
    }
    // Login only loads state.content and lets the native page navigate. It does not issue host
    // navigator commands: their collector is internal to the library. ActualWebView retains
    // native embedding, redirect interception (Allow/Reject), cookies, polling and destruction.
    // LocalWebViewFactory is deliberately untouched: overriding it selects the test-only Box.
    state.webView?.let { webView ->
        if (webView.nativeWebView !is FailedLoginNativeWebView) {
            LaunchedEffect(webView, state) {
                recoverLoginWebViewFailure(onError = { currentOnError(it) }) {
                    snapshotFlow { state.content }.collect(webView::loadContent)
                }
            }
        }
    }
    ActualWebView(
        state = state,
        modifier = modifier,
        navigator = navigator,
        webViewJsBridge = null,
        onCreated = { nativeWebView ->
            if (nativeWebView is FailedLoginNativeWebView) {
                currentOnError(nativeWebView.failure)
            } else {
                recoverLoginWebViewFailure(onError = { currentOnError(it) }) {
                    currentOnCreated(nativeWebView)
                }
            }
        },
        onDispose = {},
        factory = factory,
        content = content,
    )
}
