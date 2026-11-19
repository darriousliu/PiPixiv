// Adapted from Google Material Symbols (Sharp, view_list, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.ViewList: ImageVector
    get() = viewList

private val viewList by lazy {
    materialSymbol("Sharp.ViewList", autoMirror = true) {
        moveTo(9f, 20f)
        horizontalLineTo(22f)
        verticalLineTo(16f)
        horizontalLineTo(9f)
        verticalLineToRelative(4f)
        close()
        moveTo(2f, 8f)
        horizontalLineTo(7f)
        verticalLineTo(4f)
        horizontalLineTo(2f)
        verticalLineTo(8f)
        close()
        moveToRelative(0f, 6f)
        horizontalLineTo(7f)
        verticalLineTo(10f)
        horizontalLineTo(2f)
        verticalLineToRelative(4f)
        close()
        moveToRelative(0f, 6f)
        horizontalLineTo(7f)
        verticalLineTo(16f)
        horizontalLineTo(2f)
        verticalLineToRelative(4f)
        close()
        moveTo(9f, 14f)
        horizontalLineTo(22f)
        verticalLineTo(10f)
        horizontalLineTo(9f)
        verticalLineToRelative(4f)
        close()
        moveTo(9f, 8f)
        horizontalLineTo(22f)
        verticalLineTo(4f)
        horizontalLineTo(9f)
        verticalLineTo(8f)
        close()
    }
}
