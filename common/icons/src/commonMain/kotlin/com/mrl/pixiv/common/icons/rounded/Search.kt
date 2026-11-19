// Adapted from Google Material Symbols (Rounded, search, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Search: ImageVector
    get() = search

private val search by lazy {
    materialSymbol("Rounded.Search") {
        moveTo(9.5f, 16f)
        quadTo(6.78f, 16f, 4.89f, 14.11f)
        quadTo(3f, 12.23f, 3f, 9.5f)
        quadTo(3f, 6.77f, 4.89f, 4.89f)
        reflectiveQuadTo(9.5f, 3f)
        reflectiveQuadToRelative(4.61f, 1.89f)
        reflectiveQuadTo(16f, 9.5f)
        quadToRelative(0f, 1.1f, -0.35f, 2.07f)
        reflectiveQuadTo(14.7f, 13.3f)
        lineToRelative(5.6f, 5.6f)
        quadToRelative(0.28f, 0.28f, 0.28f, 0.7f)
        quadToRelative(0f, 0.42f, -0.28f, 0.7f)
        quadToRelative(-0.27f, 0.27f, -0.7f, 0.27f)
        reflectiveQuadTo(18.9f, 20.3f)
        lineTo(13.3f, 14.7f)
        quadToRelative(-0.75f, 0.6f, -1.72f, 0.95f)
        reflectiveQuadTo(9.5f, 16f)
        close()
        moveToRelative(0f, -2f)
        quadToRelative(1.88f, 0f, 3.19f, -1.31f)
        reflectiveQuadTo(14f, 9.5f)
        reflectiveQuadTo(12.69f, 6.31f)
        reflectiveQuadTo(9.5f, 5f)
        reflectiveQuadTo(6.31f, 6.31f)
        reflectiveQuadTo(5f, 9.5f)
        reflectiveQuadToRelative(1.31f, 3.19f)
        reflectiveQuadTo(9.5f, 14f)
        close()
    }
}
