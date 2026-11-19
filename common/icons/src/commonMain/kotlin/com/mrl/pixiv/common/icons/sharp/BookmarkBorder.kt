// Adapted from Google Material Symbols (Sharp, bookmark, FILL=0).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.BookmarkBorder: ImageVector
    get() = bookmarkBorder

private val bookmarkBorder by lazy {
    materialSymbol("Sharp.BookmarkBorder") {
        moveTo(5f, 21f)
        verticalLineTo(3f)
        horizontalLineTo(19f)
        verticalLineTo(21f)
        lineTo(12f, 18f)
        lineTo(5f, 21f)
        close()
        moveTo(7f, 17.95f)
        lineTo(12f, 15.8f)
        lineToRelative(5f, 2.15f)
        verticalLineTo(5f)
        horizontalLineTo(7f)
        verticalLineTo(17.95f)
        close()
        moveTo(7f, 5f)
        horizontalLineTo(17f)
        horizontalLineTo(12f)
        horizontalLineTo(7f)
        close()
    }
}
