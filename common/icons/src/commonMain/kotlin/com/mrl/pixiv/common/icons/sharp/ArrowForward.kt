// Adapted from Google Material Symbols (Sharp, arrow_forward, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.ArrowForward: ImageVector
    get() = arrowForward

private val arrowForward by lazy {
    materialSymbol("Sharp.ArrowForward", autoMirror = true) {
        moveTo(16.18f, 13f)
        horizontalLineTo(4f)
        verticalLineTo(11f)
        horizontalLineTo(16.18f)
        lineTo(10.58f, 5.4f)
        lineTo(12f, 4f)
        lineToRelative(8f, 8f)
        lineToRelative(-8f, 8f)
        lineTo(10.58f, 18.6f)
        lineTo(16.18f, 13f)
        close()
    }
}
