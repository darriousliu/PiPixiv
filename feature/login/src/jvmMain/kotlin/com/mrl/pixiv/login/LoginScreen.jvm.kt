package com.mrl.pixiv.login

import com.multiplatform.webview.web.NativeWebView
import com.multiplatform.webview.web.PlatformWebViewParams

internal actual fun NativeWebView.setUp() {
    // todo
}

internal actual fun createPlatformWebViewParams(onLogin: (String, String) -> Unit): PlatformWebViewParams {
    TODO("Not yet implemented")
}
