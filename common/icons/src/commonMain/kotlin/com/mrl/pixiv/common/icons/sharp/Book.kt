// Adapted from Google Material Symbols (Sharp, book, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Book: ImageVector
    get() = book

private val book by lazy {
    materialSymbol("Sharp.Book") {
        moveTo(4f, 22f)
        verticalLineTo(2f)
        horizontalLineTo(20f)
        verticalLineTo(22f)
        horizontalLineTo(4f)
        close()
        moveTo(11f, 11f)
        lineTo(13.5f, 9.5f)
        lineTo(16f, 11f)
        verticalLineTo(4f)
        horizontalLineTo(11f)
        verticalLineToRelative(7f)
        close()
    }
}
