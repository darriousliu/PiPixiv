// Adapted from Google Material Symbols (Outlined, send, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Send: ImageVector
    get() = send

private val send by lazy {
    materialSymbol("Outlined.Send", autoMirror = true) {
        moveTo(3f, 20f)
        verticalLineTo(14f)
        lineToRelative(8f, -2f)
        lineTo(3f, 10f)
        verticalLineTo(4f)
        lineToRelative(19f, 8f)
        lineTo(3f, 20f)
        close()
    }
}
