// Adapted from Google Material Symbols (Outlined, save, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Save: ImageVector
    get() = save

private val save by lazy {
    materialSymbol("Outlined.Save") {
        moveTo(21f, 7f)
        verticalLineTo(19f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(19f, 21f)
        horizontalLineTo(5f)
        quadTo(4.18f, 21f, 3.59f, 20.41f)
        reflectiveQuadTo(3f, 19f)
        verticalLineTo(5f)
        quadTo(3f, 4.17f, 3.59f, 3.59f)
        reflectiveQuadTo(5f, 3f)
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
