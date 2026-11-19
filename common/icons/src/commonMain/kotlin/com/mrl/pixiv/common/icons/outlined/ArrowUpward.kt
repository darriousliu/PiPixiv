// Adapted from Google Material Symbols (Outlined, arrow_upward, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.ArrowUpward: ImageVector
    get() = arrowUpward

private val arrowUpward by lazy {
    materialSymbol("Outlined.ArrowUpward") {
        moveTo(11f, 20f)
        verticalLineTo(7.82f)
        lineToRelative(-5.6f, 5.6f)
        lineTo(4f, 12f)
        lineTo(12f, 4f)
        lineToRelative(8f, 8f)
        lineToRelative(-1.4f, 1.42f)
        lineTo(13f, 7.82f)
        verticalLineTo(20f)
        horizontalLineTo(11f)
        close()
    }
}
