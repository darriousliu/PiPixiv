// Adapted from Google Material Symbols (Rounded, keyboard_arrow_right, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.KeyboardArrowRight: ImageVector
    get() = keyboardArrowRight

private val keyboardArrowRight by lazy {
    materialSymbol("Rounded.KeyboardArrowRight", autoMirror = true) {
        moveTo(12.6f, 12f)
        lineTo(8.7f, 8.1f)
        quadTo(8.43f, 7.82f, 8.43f, 7.4f)
        reflectiveQuadTo(8.7f, 6.7f)
        reflectiveQuadTo(9.4f, 6.43f)
        reflectiveQuadTo(10.1f, 6.7f)
        lineToRelative(4.6f, 4.6f)
        quadToRelative(0.15f, 0.15f, 0.21f, 0.33f)
        reflectiveQuadTo(14.98f, 12f)
        reflectiveQuadToRelative(-0.06f, 0.38f)
        reflectiveQuadTo(14.7f, 12.7f)
        lineToRelative(-4.6f, 4.6f)
        quadTo(9.83f, 17.58f, 9.4f, 17.58f)
        reflectiveQuadTo(8.7f, 17.3f)
        quadTo(8.43f, 17.02f, 8.43f, 16.6f)
        reflectiveQuadTo(8.7f, 15.9f)
        lineTo(12.6f, 12f)
        close()
    }
}
