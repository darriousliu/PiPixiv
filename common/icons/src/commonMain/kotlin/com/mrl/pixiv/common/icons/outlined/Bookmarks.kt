// Adapted from Google Material Symbols (Outlined, bookmarks, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Bookmarks: ImageVector
    get() = bookmarks

private val bookmarks by lazy {
    materialSymbol("Outlined.Bookmarks") {
        moveTo(4f, 22f)
        verticalLineTo(8f)
        quadTo(4f, 7.18f, 4.59f, 6.59f)
        reflectiveQuadTo(6f, 6f)
        horizontalLineToRelative(8f)
        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
        quadTo(16f, 7.18f, 16f, 8f)
        verticalLineTo(22f)
        lineTo(10f, 19f)
        lineTo(4f, 22f)
        close()
        moveTo(18f, 18f)
        verticalLineTo(4f)
        horizontalLineTo(7f)
        verticalLineTo(2f)
        horizontalLineTo(18f)
        quadToRelative(0.82f, 0f, 1.41f, 0.59f)
        reflectiveQuadTo(20f, 4f)
        verticalLineTo(18f)
        horizontalLineTo(18f)
        close()
    }
}
