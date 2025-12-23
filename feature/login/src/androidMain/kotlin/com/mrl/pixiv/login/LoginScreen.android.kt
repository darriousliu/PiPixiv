package com.mrl.pixiv.login

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import co.touchlab.kermit.Logger
import com.multiplatform.webview.web.AccompanistWebViewClient
import com.multiplatform.webview.web.NativeWebView
import com.multiplatform.webview.web.PlatformWebViewParams
import io.ktor.http.Url

@SuppressLint("SetJavaScriptEnabled")
internal actual fun NativeWebView.setUp() {
    settings.apply {
        javaScriptEnabled = true
        cacheMode = WebSettings.LOAD_DEFAULT
        userAgentString = userAgentString.replace("; wv", "")
    }
}

internal actual fun createPlatformWebViewParams(
    onLogin: (String, String) -> Unit,
): PlatformWebViewParams {
    return PlatformWebViewParams(
        client = object : AccompanistWebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                Logger.d("LoginScreen") { "shouldOverrideUrlLoading: ${request?.url}" }
                val codePair = checkUri(Url(request?.url.toString()))
                if (codePair != null) {
                    onLogin(codePair.first, codePair.second)
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        },
    )
}