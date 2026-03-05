package com.mrl.pixiv.common.util

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import com.mrl.pixiv.common.compose.layout.isWidthCompact

@Composable
actual fun currentOrientation(): Orientation {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    return when {
        windowAdaptiveInfo.isWidthCompact -> Orientation.PORTRAIT
        else -> Orientation.LANDSCAPE
    }
}