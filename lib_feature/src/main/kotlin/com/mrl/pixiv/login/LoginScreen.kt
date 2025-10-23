package com.mrl.pixiv.login

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.common.viewmodel.asState
import com.multiplatform.webview.web.AccompanistWebViewClient
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.PlatformWebViewParams
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    startUrl: String,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
    navigationManager: NavigationManager = koinInject(),
) {
    val state = viewModel.asState()
    val webViewState = rememberWebViewState(url = startUrl)
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

                LoadingState.Initializing -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is LoadingState.Loading -> LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = { loadingState.progress }
                )
            }
            WebView(
                modifier = Modifier.weight(1f),
                state = webViewState,
                onCreated = { webview ->
                    webview.settings.apply {
                        javaScriptEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        userAgentString = userAgentString.replace("; wv", "")
                    }
                },
                platformWebViewParams = PlatformWebViewParams(
                    client = object : AccompanistWebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            Log.d("LoginScreen", "shouldOverrideUrlLoading: ${request?.url}")
                            val codePair = checkUri(request?.url!!)
                            if (codePair != null) {
                                viewModel.dispatch(
                                    LoginAction.Login(codePair.first, codePair.second)
                                )
                                return true
                            }
                            return super.shouldOverrideUrlLoading(view, request)
                        }
                    },
                )
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
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}