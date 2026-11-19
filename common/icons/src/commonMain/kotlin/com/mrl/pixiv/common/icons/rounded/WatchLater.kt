// Adapted from Google Material Symbols (Rounded, watch_later, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.WatchLater: ImageVector
    get() = watchLater

private val watchLater by lazy {
    materialSymbol("Rounded.WatchLater") {
        moveTo(13f, 11.6f)
        verticalLineTo(8f)
        quadTo(13f, 7.57f, 12.71f, 7.29f)
        reflectiveQuadTo(12f, 7f)
        reflectiveQuadTo(11.29f, 7.29f)
        reflectiveQuadTo(11f, 8f)
        verticalLineToRelative(3.97f)
        quadToRelative(0f, 0.2f, 0.08f, 0.39f)
        reflectiveQuadTo(11.3f, 12.7f)
        lineTo(14.6f, 16f)
        quadToRelative(0.27f, 0.27f, 0.7f, 0.27f)
        reflectiveQuadTo(16f, 16f)
        quadToRelative(0.28f, -0.28f, 0.28f, -0.7f)
        quadToRelative(0f, -0.42f, -0.28f, -0.7f)
        lineToRelative(-3f, -3f)
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
