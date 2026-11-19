// Adapted from Google Material Symbols (Sharp, arrow_forward_ios, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.ArrowForwardIos: ImageVector
    get() = arrowForwardIos

private val arrowForwardIos by lazy {
    materialSymbol("Sharp.ArrowForwardIos", autoMirror = true) {
        moveTo(8.03f, 22f)
        lineTo(6.25f, 20.23f)
        lineTo(14.48f, 12f)
        lineTo(6.25f, 3.77f)
        lineTo(8.03f, 2f)
        lineToRelative(10f, 10f)
        lineToRelative(-10f, 10f)
        close()
    }
}
