// Adapted from Google Material Symbols (Outlined, book, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Book: ImageVector
    get() = book

private val book by lazy {
    materialSymbol("Outlined.Book") {
        moveTo(6f, 22f)
        quadTo(5.18f, 22f, 4.59f, 21.41f)
        reflectiveQuadTo(4f, 20f)
        verticalLineTo(4f)
        quadTo(4f, 3.17f, 4.59f, 2.59f)
        reflectiveQuadTo(6f, 2f)
        horizontalLineTo(18f)
        quadToRelative(0.82f, 0f, 1.41f, 0.59f)
        reflectiveQuadTo(20f, 4f)
        verticalLineTo(20f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(18f, 22f)
        horizontalLineTo(6f)
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
