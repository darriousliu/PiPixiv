// Adapted from Google Material Symbols (Sharp, comment, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Comment: ImageVector
    get() = comment

private val comment by lazy {
    materialSymbol("Sharp.Comment", autoMirror = true) {
        moveTo(6f, 14f)
        horizontalLineTo(18f)
        verticalLineTo(12f)
        horizontalLineTo(6f)
        verticalLineToRelative(2f)
        close()
        moveTo(6f, 11f)
        horizontalLineTo(18f)
        verticalLineTo(9f)
        horizontalLineTo(6f)
        verticalLineToRelative(2f)
        close()
        moveTo(6f, 8f)
        horizontalLineTo(18f)
        verticalLineTo(6f)
        horizontalLineTo(6f)
        verticalLineTo(8f)
        close()
        moveTo(2f, 18f)
        verticalLineTo(2f)
        horizontalLineTo(22f)
        verticalLineTo(22f)
        lineTo(18f, 18f)
        horizontalLineTo(2f)
        close()
    }
}
