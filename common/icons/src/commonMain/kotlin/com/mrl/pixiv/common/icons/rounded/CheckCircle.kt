// Adapted from Google Material Symbols (Rounded, check_circle, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.CheckCircle: ImageVector
    get() = checkCircle

private val checkCircle by lazy {
    materialSymbol("Rounded.CheckCircle") {
        moveTo(10.6f, 13.8f)
        lineTo(8.45f, 11.65f)
        quadTo(8.18f, 11.38f, 7.75f, 11.38f)
        quadToRelative(-0.42f, 0f, -0.7f, 0.27f)
        quadToRelative(-0.28f, 0.28f, -0.28f, 0.7f)
        quadToRelative(0f, 0.42f, 0.28f, 0.7f)
        lineTo(9.9f, 15.9f)
        quadToRelative(0.3f, 0.3f, 0.7f, 0.3f)
        reflectiveQuadToRelative(0.7f, -0.3f)
        lineToRelative(5.65f, -5.65f)
        quadToRelative(0.28f, -0.28f, 0.28f, -0.7f)
        quadToRelative(0f, -0.42f, -0.28f, -0.7f)
        quadTo(16.68f, 8.57f, 16.25f, 8.57f)
        reflectiveQuadToRelative(-0.7f, 0.28f)
        lineTo(10.6f, 13.8f)
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
