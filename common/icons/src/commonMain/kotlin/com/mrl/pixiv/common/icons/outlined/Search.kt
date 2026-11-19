// Adapted from Google Material Symbols (Outlined, search, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Search: ImageVector
    get() = search

private val search by lazy {
    materialSymbol("Outlined.Search") {
        moveTo(19.6f, 21f)
        lineTo(13.3f, 14.7f)
        quadToRelative(-0.75f, 0.6f, -1.72f, 0.95f)
        reflectiveQuadTo(9.5f, 16f)
        quadTo(6.78f, 16f, 4.89f, 14.11f)
        quadTo(3f, 12.23f, 3f, 9.5f)
        quadTo(3f, 6.77f, 4.89f, 4.89f)
        reflectiveQuadTo(9.5f, 3f)
        reflectiveQuadToRelative(4.61f, 1.89f)
        reflectiveQuadTo(16f, 9.5f)
        quadToRelative(0f, 1.1f, -0.35f, 2.07f)
        reflectiveQuadTo(14.7f, 13.3f)
        lineTo(21f, 19.6f)
        lineTo(19.6f, 21f)
        close()
        moveTo(9.5f, 14f)
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
