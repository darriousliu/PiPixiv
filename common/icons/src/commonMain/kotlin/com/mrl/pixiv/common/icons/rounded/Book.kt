// Adapted from Google Material Symbols (Rounded, book, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Book: ImageVector
    get() = book

private val book by lazy {
    materialSymbol("Rounded.Book") {
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
        moveTo(11f, 4f)
        verticalLineToRelative(6.13f)
        quadToRelative(0f, 0.3f, 0.24f, 0.44f)
        reflectiveQuadToRelative(0.51f, -0.01f)
        lineTo(12.98f, 9.82f)
        quadTo(13.23f, 9.67f, 13.49f, 9.67f)
        reflectiveQuadTo(14f, 9.82f)
        lineToRelative(1.23f, 0.72f)
        quadToRelative(0.27f, 0.15f, 0.52f, 0.01f)
        reflectiveQuadTo(16f, 10.13f)
        verticalLineTo(4f)
        horizontalLineTo(11f)
        close()
    }
}
