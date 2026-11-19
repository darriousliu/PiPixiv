// Adapted from Google Material Symbols (Sharp, save, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Save: ImageVector
    get() = save

private val save by lazy {
    materialSymbol("Sharp.Save") {
        moveTo(21f, 7f)
        verticalLineTo(21f)
        horizontalLineTo(3f)
        verticalLineTo(3f)
        horizontalLineTo(17f)
        lineToRelative(4f, 4f)
        close()
        moveTo(14.13f, 17.13f)
        quadTo(15f, 16.25f, 15f, 15f)
        reflectiveQuadTo(14.13f, 12.88f)
        reflectiveQuadTo(12f, 12f)
        reflectiveQuadTo(9.88f, 12.88f)
        reflectiveQuadTo(9f, 15f)
        reflectiveQuadToRelative(0.88f, 2.13f)
        reflectiveQuadTo(12f, 18f)
        reflectiveQuadToRelative(2.13f, -0.88f)
        close()
        moveTo(6f, 10f)
        horizontalLineToRelative(9f)
        verticalLineTo(6f)
        horizontalLineTo(6f)
        verticalLineToRelative(4f)
        close()
    }
}
