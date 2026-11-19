// Adapted from Google Material Symbols (Sharp, warning, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Warning: ImageVector
    get() = warning

private val warning by lazy {
    materialSymbol("Sharp.Warning") {
        moveTo(1f, 21f)
        lineTo(12f, 2f)
        lineTo(23f, 21f)
        horizontalLineTo(1f)
        close()
        moveTo(12.71f, 17.71f)
        quadTo(13f, 17.43f, 13f, 17f)
        reflectiveQuadTo(12.71f, 16.29f)
        reflectiveQuadTo(12f, 16f)
        reflectiveQuadToRelative(-0.71f, 0.29f)
        reflectiveQuadTo(11f, 17f)
        reflectiveQuadToRelative(0.29f, 0.71f)
        reflectiveQuadTo(12f, 18f)
        reflectiveQuadToRelative(0.71f, -0.29f)
        close()
        moveTo(11f, 15f)
        horizontalLineToRelative(2f)
        verticalLineTo(10f)
        horizontalLineTo(11f)
        verticalLineToRelative(5f)
        close()
    }
}
