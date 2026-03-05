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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.common.viewmodel.asState
import io.github.kdroidfilter.webview.request.RequestInterceptor
import io.github.kdroidfilter.webview.request.WebRequest
import io.github.kdroidfilter.webview.request.WebRequestInterceptResult
import io.github.kdroidfilter.webview.web.LoadingState
import io.github.kdroidfilter.webview.web.NativeWebView
import io.github.kdroidfilter.webview.web.WebView
import io.github.kdroidfilter.webview.web.WebViewNavigator
import io.github.kdroidfilter.webview.web.rememberWebViewNavigator
import io.github.kdroidfilter.webview.web.rememberWebViewState
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    startUrl: String,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
    navigationManager: NavigationManager = koinInject(),
) {
    val state = viewModel.asState()
    val webViewState = rememberWebViewState(url = startUrl)
    val webViewNavigator = rememberWebViewNavigator(
        requestInterceptor = object : RequestInterceptor {
            override fun onInterceptUrlRequest(
                request: WebRequest,
                navigator: WebViewNavigator
            ): WebRequestInterceptResult {
                Logger.d("LoginScreen") { "shouldOverrideUrlLoading: ${request.url}" }
                val codePair = checkUri(request.url)
                if (codePair != null) {
                    viewModel.dispatch(LoginAction.Login(codePair.first, codePair.second))
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
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navigationManager.popBackStack()
                        },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        }
    ) {
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
            WebView(
                modifier = Modifier.fillMaxSize(1f),
                state = webViewState,
                navigator = webViewNavigator,
                onCreated = { webview ->
                    webview.setUp()
                },
            )
        }
    }
    if (state.loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                .throttleClick()
        ) {
            CircularWavyProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

internal expect fun NativeWebView.setUp()
