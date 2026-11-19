// Adapted from Google Material Symbols (Rounded, expand_less, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.ExpandLess: ImageVector
    get() = expandLess

private val expandLess by lazy {
    materialSymbol("Rounded.ExpandLess") {
        moveTo(12f, 10.77f)
        lineToRelative(-3.9f, 3.9f)
        quadTo(7.83f, 14.95f, 7.4f, 14.95f)
        reflectiveQuadTo(6.7f, 14.68f)
        reflectiveQuadTo(6.43f, 13.98f)
        reflectiveQuadTo(6.7f, 13.27f)
        lineToRelative(4.6f, -4.6f)
        quadTo(11.45f, 8.52f, 11.63f, 8.46f)
        reflectiveQuadTo(12f, 8.4f)
        reflectiveQuadToRelative(0.38f, 0.06f)
        reflectiveQuadTo(12.7f, 8.67f)
        lineToRelative(4.6f, 4.6f)
        quadToRelative(0.27f, 0.28f, 0.27f, 0.7f)
        reflectiveQuadToRelative(-0.27f, 0.7f)
        reflectiveQuadToRelative(-0.7f, 0.28f)
        reflectiveQuadTo(15.9f, 14.68f)
        lineTo(12f, 10.77f)
        close()
    }
}
