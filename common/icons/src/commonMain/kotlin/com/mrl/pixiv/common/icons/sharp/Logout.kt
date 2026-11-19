// Adapted from Google Material Symbols (Sharp, logout, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Logout: ImageVector
    get() = logout

private val logout by lazy {
    materialSymbol("Sharp.Logout", autoMirror = true) {
        moveTo(3f, 21f)
        verticalLineTo(3f)
        horizontalLineToRelative(9f)
        verticalLineTo(5f)
        horizontalLineTo(5f)
        verticalLineTo(19f)
        horizontalLineToRelative(7f)
        verticalLineToRelative(2f)
        horizontalLineTo(3f)
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
