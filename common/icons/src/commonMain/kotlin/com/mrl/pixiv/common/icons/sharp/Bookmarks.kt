// Adapted from Google Material Symbols (Sharp, bookmarks, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Bookmarks: ImageVector
    get() = bookmarks

private val bookmarks by lazy {
    materialSymbol("Sharp.Bookmarks") {
        moveTo(4f, 22f)
        verticalLineTo(6f)
        horizontalLineTo(16f)
        verticalLineTo(22f)
        lineTo(10f, 19f)
        lineTo(4f, 22f)
        close()
        moveTo(18f, 18f)
        verticalLineTo(4f)
        horizontalLineTo(7f)
        verticalLineTo(2f)
        horizontalLineTo(20f)
        verticalLineTo(18f)
        horizontalLineTo(18f)
        close()
    }
}
