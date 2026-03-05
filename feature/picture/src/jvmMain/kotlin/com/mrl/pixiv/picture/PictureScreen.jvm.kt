package com.mrl.pixiv.picture

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mrl.pixiv.common.util.throttleClick

@Composable
internal actual fun Modifier.clickWithPermission(onClick: () -> Unit): Modifier {
    return this.throttleClick { onClick() }
}