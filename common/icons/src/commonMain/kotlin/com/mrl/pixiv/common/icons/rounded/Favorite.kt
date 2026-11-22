// Adapted from Google Material Symbols (Rounded, favorite, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Favorite: ImageVector
    get() = favorite

private val favorite by lazy {
    materialSymbol("Rounded.Favorite") {
        moveTo(11.29f, 20.2f)
        quadTo(10.93f, 20.08f, 10.65f, 19.8f)
        lineTo(8.93f, 18.23f)
        quadTo(6.28f, 15.8f, 4.14f, 13.41f)
        quadTo(2f, 11.02f, 2f, 8.15f)
        quadTo(2f, 5.8f, 3.58f, 4.22f)
        reflectiveQuadTo(7.5f, 2.65f)
        quadToRelative(1.33f, 0f, 2.5f, 0.56f)
        reflectiveQuadToRelative(2f, 1.54f)
        quadTo(12.83f, 3.77f, 14f, 3.21f)
        reflectiveQuadTo(16.5f, 2.65f)
        quadToRelative(2.35f, 0f, 3.93f, 1.57f)
        reflectiveQuadTo(22f, 8.15f)
        quadToRelative(0f, 2.88f, -2.13f, 5.28f)
        reflectiveQuadToRelative(-4.82f, 4.83f)
        lineToRelative(-1.7f, 1.55f)
        quadToRelative(-0.28f, 0.27f, -0.64f, 0.4f)
        reflectiveQuadTo(12f, 20.33f)
        reflectiveQuadTo(11.29f, 20.2f)
        close()
    }
}
