// Adapted from Google Material Symbols (Sharp, watch_later, FILL=0).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp.unfilled

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.WatchLater: ImageVector
    get() = watchLater

private val watchLater by lazy {
    materialSymbol("Sharp.WatchLater.0") {
        moveTo(15.3f, 16.7f)
        lineToRelative(1.4f, -1.4f)
        lineTo(13f, 11.6f)
        verticalLineTo(7f)
        horizontalLineTo(11f)
        verticalLineToRelative(5.4f)
        lineToRelative(4.3f, 4.3f)
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
        moveTo(12f, 12f)
        close()
        moveToRelative(0f, 8f)
        quadToRelative(3.33f, 0f, 5.66f, -2.34f)
        reflectiveQuadTo(20f, 12f)
        quadTo(20f, 8.67f, 17.66f, 6.34f)
        reflectiveQuadTo(12f, 4f)
        quadTo(8.68f, 4f, 6.34f, 6.34f)
        reflectiveQuadTo(4f, 12f)
        reflectiveQuadToRelative(2.34f, 5.66f)
        reflectiveQuadTo(12f, 20f)
        close()
    }
}
