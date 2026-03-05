package com.mrl.pixiv.common.toast

import androidx.compose.runtime.Composable

sealed class ToastMessage {
    data class Compose(val content: @Composable () -> Unit) : ToastMessage()
}