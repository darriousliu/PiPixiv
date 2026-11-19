// Adapted from Google Material Symbols (Rounded, play_circle, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.PlayCircle: ImageVector
    get() = playCircle

private val playCircle by lazy {
    materialSymbol("Rounded.PlayCircle") {
        moveTo(10.65f, 15.75f)
        lineToRelative(4.88f, -3.13f)
        quadTo(15.88f, 12.4f, 15.88f, 12f)
        reflectiveQuadTo(15.53f, 11.38f)
        lineTo(10.65f, 8.25f)
        quadTo(10.28f, 8f, 9.89f, 8.21f)
        reflectiveQuadTo(9.5f, 8.88f)
        verticalLineToRelative(6.25f)
        quadToRelative(0f, 0.45f, 0.39f, 0.66f)
        reflectiveQuadToRelative(0.76f, -0.04f)
        close()
        moveTo(12f, 22f)
        quadTo(9.93f, 22f, 8.1f, 21.21f)
        quadTo(6.28f, 20.43f, 4.93f, 19.08f)
        quadTo(3.58f, 17.73f, 2.79f, 15.9f)
        reflectiveQuadTo(2f, 12f)
        quadTo(2f, 9.92f, 2.79f, 8.1f)
        quadTo(3.58f, 6.27f, 4.93f, 4.93f)
        quadTo(6.28f, 3.57f, 8.1f, 2.79f)
        quadTo(9.93f, 2f, 12f, 2f)
        reflectiveQuadToRelative(3.9f, 0.79f)
        reflectiveQuadToRelative(3.17f, 2.14f)
        quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
        quadTo(22f, 9.92f, 22f, 12f)
        reflectiveQuadToRelative(-0.79f, 3.9f)
        reflectiveQuadToRelative(-2.14f, 3.17f)
        quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
        reflectiveQuadTo(12f, 22f)
        close()
    }
}
