// Adapted from Google Material Symbols (Rounded, bookmarks, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Bookmarks: ImageVector
    get() = bookmarks

private val bookmarks by lazy {
    materialSymbol("Rounded.Bookmarks") {
        moveTo(10f, 19f)
        lineTo(5.45f, 21.28f)
        quadTo(4.95f, 21.53f, 4.48f, 21.24f)
        reflectiveQuadTo(4f, 20.38f)
        verticalLineTo(8f)
        quadTo(4f, 7.18f, 4.59f, 6.59f)
        reflectiveQuadTo(6f, 6f)
        horizontalLineToRelative(8f)
        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
        quadTo(16f, 7.18f, 16f, 8f)
        verticalLineTo(20.38f)
        quadToRelative(0f, 0.57f, -0.47f, 0.86f)
        reflectiveQuadToRelative(-0.97f, 0.04f)
        lineTo(10f, 19f)
        close()
        moveToRelative(8.29f, -1.29f)
        quadTo(18f, 17.43f, 18f, 17f)
        verticalLineTo(4f)
        horizontalLineTo(8f)
        quadTo(7.58f, 4f, 7.29f, 3.71f)
        reflectiveQuadTo(7f, 3f)
        quadTo(7f, 2.57f, 7.29f, 2.29f)
        reflectiveQuadTo(8f, 2f)
        horizontalLineTo(18f)
        quadToRelative(0.82f, 0f, 1.41f, 0.59f)
        reflectiveQuadTo(20f, 4f)
        verticalLineTo(17f)
        quadToRelative(0f, 0.43f, -0.29f, 0.71f)
        reflectiveQuadTo(19f, 18f)
        reflectiveQuadTo(18.29f, 17.71f)
        close()
    }
}
