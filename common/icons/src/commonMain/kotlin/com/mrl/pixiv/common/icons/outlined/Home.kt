// Adapted from Google Material Symbols (Outlined, home, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Home: ImageVector
    get() = home

private val home by lazy {
    materialSymbol("Outlined.Home") {
        moveTo(4f, 21f)
        verticalLineTo(9f)
        lineTo(12f, 3f)
        lineToRelative(8f, 6f)
        verticalLineTo(21f)
        horizontalLineTo(14f)
        verticalLineTo(14f)
        horizontalLineTo(10f)
        verticalLineToRelative(7f)
        horizontalLineTo(4f)
        close()
    }
}
