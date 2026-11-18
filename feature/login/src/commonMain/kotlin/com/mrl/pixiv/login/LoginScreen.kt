package com.mrl.pixiv.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.analytics.logException
import com.mrl.pixiv.common.compose.ui.ContentCrashState
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import com.mrl.pixiv.common.network.networkSettings
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.Platform
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.platform
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.strings.login_other_methods
import com.mrl.pixiv.strings.login_webview_failed
import com.mrl.pixiv.strings.login_webview_route_unavailable
import dev.nucleusframework.webview.request.RequestInterceptor
import dev.nucleusframework.webview.request.WebRequest
import dev.nucleusframework.webview.request.WebRequestInterceptResult
import dev.nucleusframework.webview.web.LoadingState
import dev.nucleusframework.webview.web.NativeWebView
import dev.nucleusframework.webview.web.WebViewNavigator
import dev.nucleusframework.webview.web.rememberWebViewNavigator
import dev.nucleusframework.webview.web.rememberWebViewState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private enum class LoginWebViewFailure(val message: StringResource) {
    Runtime(RStrings.login_webview_failed),
    NetworkRoute(RStrings.login_webview_route_unavailable),
}

@Composable
fun LoginScreen(
    startUrl: String,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val state = viewModel.asState()
    var webViewFailure by rememberSaveable(startUrl) { mutableStateOf<LoginWebViewFailure?>(null) }
    val webViewFailed = webViewFailure != null
    val networkPreference = remember(startUrl) { requireUserPreferenceValue.networkSettings() }
    val bypassSetting = when (val configured = networkPreference.bypassSetting) {
        is BypassSetting.ECH -> BypassSetting.Direct
        else -> configured
    }
    val nativeDesktopOverlay = platform is Platform.Desktop
    val webViewState = rememberWebViewState(url = startUrl)
    val webViewNavigator = rememberWebViewNavigator(
        requestInterceptor = object : RequestInterceptor {
            override fun onInterceptUrlRequest(
                request: WebRequest,
                navigator: WebViewNavigator
            ): WebRequestInterceptResult {
                Logger.d(tag = "LoginScreen") { "shouldOverrideUrlLoading: ${request.url}" }
                val codePair = checkUri(request.url)
                if (codePair != null) {
                    viewModel.dispatch(LoginAction.Login(codePair.first, codePair.second, networkPreference))
                    return WebRequestInterceptResult.Reject
                }
                return WebRequestInterceptResult.Allow
            }
        }
    )
    val loadingState = webViewState.loadingState

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect {
            when (it) {
                is LoginEvent.NavigateToMain -> {
                    navigationManager.loginToMainScreen()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .imePadding(),
        topBar = {
            Box {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(
                            onClick = { navigationManager.popBackStack() },
                            enabled = webViewFailed || !state.loading,
                            shapes = IconButtonDefaults.shapes(),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                )
                if (!webViewFailed && nativeDesktopOverlay && state.loading) {
                    LoginLoadingOverlay(Modifier.matchParentSize(), showIndicator = false)
                }
            }
        }
    ) {
        val failure = webViewFailure
        if (failure != null) {
            LoginWebViewError(
                message = failure.message,
                onTryOtherMethods = navigationManager::navigateToLoginOptionScreen,
                modifier = Modifier.padding(it),
            )
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            when (loadingState) {
                LoadingState.Finished -> {}

                LoadingState.Initializing -> LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                is LoadingState.Loading -> LinearWavyProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = { loadingState.progress }
                )
            }
            LoginWebView(
                modifier = Modifier.fillMaxSize(1f),
                state = webViewState,
                navigator = webViewNavigator,
                bypassSetting = bypassSetting,
                onError = { error ->
                    if (webViewFailure == null) {
                        webViewFailure = if (error is WebLoginRouteUnavailableException) {
                            LoginWebViewFailure.NetworkRoute
                        } else {
                            logException(error)
                            LoginWebViewFailure.Runtime
                        }
                    }
                },
                onCreated = { webview ->
                    webview.setUp()
                },
                content = {
                    // Native desktop WebViews need their own Compose overlay surface.
                    if (nativeDesktopOverlay && state.loading) LoginLoadingOverlay()
                },
            )
        }
    }
    if (!webViewFailed && !nativeDesktopOverlay && state.loading) {
        LoginLoadingOverlay()
    }
}

@Composable
internal fun LoginWebViewError(
    message: StringResource,
    onTryOtherMethods: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ContentCrashState(
        message = stringResource(message),
        onAction = onTryOtherMethods,
        modifier = modifier,
        actionText = stringResource(RStrings.login_other_methods),
    )
}

@Composable
private fun LoginLoadingOverlay(
    modifier: Modifier = Modifier.fillMaxSize(),
    showIndicator: Boolean = true,
) {
    Box(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            .throttleClick(),
    ) {
        if (showIndicator) {
            CircularWavyProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

internal expect fun NativeWebView.setUp()
