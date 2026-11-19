package com.mrl.pixiv.novel

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import com.mrl.pixiv.common.util.Platform
import com.mrl.pixiv.common.util.platform

@Composable
internal expect fun novelReaderContentInsets(): WindowInsets

@Composable
internal fun novelReaderScaffoldInsets(): WindowInsets = when (platform) {
    // 正文和浮层各自应用稳定的垂直边距，Scaffold 只处理横向安全区。
    is Platform.Android -> novelReaderContentInsets().only(WindowInsetsSides.Horizontal)
    // 适配 iPhone 横屏的灵动岛。
    Platform.Apple.IPhoneOS -> ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal)
    else -> ScaffoldDefaults.contentWindowInsets.exclude(novelReaderContentInsets())
}
