package com.mrl.pixiv.common.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication
import platform.UIKit.setStatusBarHidden

@Composable
actual fun StatusBarVisibilityEffect(hidden: Boolean) {
    DisposableEffect(hidden) {
        setStatusBarHidden(hidden)
        onDispose {
            setStatusBarHidden(false)
        }
    }
}

@Suppress("DEPRECATION")
private fun setStatusBarHidden(hidden: Boolean) {
    UIApplication.sharedApplication.setStatusBarHidden(
        hidden = hidden,
        animated = true,
    )
}
