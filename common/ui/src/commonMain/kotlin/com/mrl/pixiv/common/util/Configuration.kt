package com.mrl.pixiv.common.util

import androidx.compose.runtime.Composable

enum class Orientation {
    PORTRAIT, LANDSCAPE
}

@Composable
expect fun currentOrientation(): Orientation