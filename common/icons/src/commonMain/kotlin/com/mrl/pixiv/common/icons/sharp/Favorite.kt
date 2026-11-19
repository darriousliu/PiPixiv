// Adapted from Google Material Symbols (Sharp, favorite, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Favorite: ImageVector
    get() = favorite

private val favorite by lazy {
    materialSymbol("Sharp.Favorite") {
        moveTo(12f, 21f)
        lineTo(10.55f, 19.7f)
        quadTo(8.03f, 17.43f, 6.38f, 15.78f)
        quadTo(4.73f, 14.13f, 3.75f, 12.81f)
        quadTo(2.78f, 11.5f, 2.39f, 10.4f)
        reflectiveQuadTo(2f, 8.15f)
        quadTo(2f, 5.8f, 3.58f, 4.22f)
        reflectiveQuadTo(7.5f, 2.65f)
        quadToRelative(1.3f, 0f, 2.48f, 0.55f)
        reflectiveQuadTo(12f, 4.75f)
        quadToRelative(0.85f, -1f, 2.03f, -1.55f)
        reflectiveQuadTo(16.5f, 2.65f)
        quadToRelative(2.35f, 0f, 3.93f, 1.57f)
        reflectiveQuadTo(22f, 8.15f)
        quadTo(22f, 9.3f, 21.61f, 10.4f)
        reflectiveQuadToRelative(-1.36f, 2.41f)
        quadToRelative(-0.97f, 1.31f, -2.63f, 2.96f)
        quadToRelative(-1.65f, 1.65f, -4.17f, 3.92f)
        lineTo(12f, 21f)
        close()
    }
}
