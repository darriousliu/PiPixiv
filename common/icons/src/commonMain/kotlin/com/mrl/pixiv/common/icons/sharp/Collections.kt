// Adapted from Google Material Symbols (Sharp, collections, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Collections: ImageVector
    get() = collections

private val collections by lazy {
    materialSymbol("Sharp.Collections") {
        moveTo(9f, 14f)
        horizontalLineTo(19f)
        lineTo(15.55f, 9.5f)
        lineToRelative(-2.3f, 3f)
        lineToRelative(-1.55f, -2f)
        lineTo(9f, 14f)
        close()
        moveTo(6f, 18f)
        verticalLineTo(2f)
        horizontalLineTo(22f)
        verticalLineTo(18f)
        horizontalLineTo(6f)
        close()
        moveTo(2f, 22f)
        verticalLineTo(6f)
        horizontalLineTo(4f)
        verticalLineTo(20f)
        horizontalLineTo(18f)
        verticalLineToRelative(2f)
        horizontalLineTo(2f)
        close()
    }
}
