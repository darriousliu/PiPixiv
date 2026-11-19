// Adapted from Google Material Symbols (Sharp, dns, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Dns: ImageVector
    get() = dns

private val dns by lazy {
    materialSymbol("Sharp.Dns") {
        moveTo(7.5f, 9f)
        quadTo(8.13f, 9f, 8.56f, 8.56f)
        reflectiveQuadTo(9f, 7.5f)
        reflectiveQuadTo(8.56f, 6.44f)
        reflectiveQuadTo(7.5f, 6f)
        reflectiveQuadTo(6.44f, 6.44f)
        reflectiveQuadTo(6f, 7.5f)
        reflectiveQuadTo(6.44f, 8.56f)
        reflectiveQuadTo(7.5f, 9f)
        close()
        moveToRelative(0f, 10f)
        quadToRelative(0.63f, 0f, 1.06f, -0.44f)
        reflectiveQuadTo(9f, 17.5f)
        reflectiveQuadTo(8.56f, 16.44f)
        reflectiveQuadTo(7.5f, 16f)
        reflectiveQuadTo(6.44f, 16.44f)
        reflectiveQuadTo(6f, 17.5f)
        reflectiveQuadToRelative(0.44f, 1.06f)
        reflectiveQuadTo(7.5f, 19f)
        close()
        moveTo(3f, 12f)
        verticalLineTo(3f)
        horizontalLineTo(21f)
        verticalLineToRelative(9f)
        horizontalLineTo(3f)
        close()
        moveTo(3f, 22f)
        verticalLineTo(13f)
        horizontalLineTo(21f)
        verticalLineToRelative(9f)
        horizontalLineTo(3f)
        close()
    }
}
