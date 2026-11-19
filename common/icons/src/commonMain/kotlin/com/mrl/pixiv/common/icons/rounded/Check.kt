// Adapted from Google Material Symbols (Rounded, check, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Check: ImageVector
    get() = check

private val check by lazy {
    materialSymbol("Rounded.Check") {
        moveTo(9.55f, 15.15f)
        lineTo(18.03f, 6.68f)
        quadToRelative(0.3f, -0.3f, 0.7f, -0.3f)
        reflectiveQuadToRelative(0.7f, 0.3f)
        quadToRelative(0.3f, 0.3f, 0.3f, 0.71f)
        reflectiveQuadTo(19.43f, 8.1f)
        lineToRelative(-9.18f, 9.2f)
        quadToRelative(-0.3f, 0.3f, -0.7f, 0.3f)
        reflectiveQuadTo(8.85f, 17.3f)
        lineTo(4.55f, 13f)
        quadTo(4.25f, 12.7f, 4.26f, 12.29f)
        reflectiveQuadTo(4.58f, 11.58f)
        reflectiveQuadToRelative(0.71f, -0.3f)
        reflectiveQuadTo(6f, 11.58f)
        lineToRelative(3.55f, 3.58f)
        close()
    }
}
