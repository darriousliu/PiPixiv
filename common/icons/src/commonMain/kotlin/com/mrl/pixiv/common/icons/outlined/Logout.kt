// Adapted from Google Material Symbols (Outlined, logout, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Logout: ImageVector
    get() = logout

private val logout by lazy {
    materialSymbol("Outlined.Logout", autoMirror = true) {
        moveTo(5f, 21f)
        quadTo(4.18f, 21f, 3.59f, 20.41f)
        reflectiveQuadTo(3f, 19f)
        verticalLineTo(5f)
        quadTo(3f, 4.17f, 3.59f, 3.59f)
        reflectiveQuadTo(5f, 3f)
        horizontalLineToRelative(7f)
        verticalLineTo(5f)
        horizontalLineTo(5f)
        verticalLineTo(19f)
        horizontalLineToRelative(7f)
        verticalLineToRelative(2f)
        horizontalLineTo(5f)
        close()
        moveTo(16f, 17f)
        lineTo(14.63f, 15.55f)
        lineTo(17.18f, 13f)
        horizontalLineTo(9f)
        verticalLineTo(11f)
        horizontalLineToRelative(8.18f)
        lineTo(14.63f, 8.45f)
        lineTo(16f, 7f)
        lineToRelative(5f, 5f)
        lineToRelative(-5f, 5f)
        close()
    }
}
