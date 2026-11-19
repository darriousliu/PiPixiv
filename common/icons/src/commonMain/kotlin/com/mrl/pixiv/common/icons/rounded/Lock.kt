// Adapted from Google Material Symbols (Rounded, lock, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Lock: ImageVector
    get() = lock

private val lock by lazy {
    materialSymbol("Rounded.Lock") {
        moveTo(6f, 22f)
        quadTo(5.18f, 22f, 4.59f, 21.41f)
        reflectiveQuadTo(4f, 20f)
        verticalLineTo(10f)
        quadTo(4f, 9.17f, 4.59f, 8.59f)
        reflectiveQuadTo(6f, 8f)
        horizontalLineTo(7f)
        verticalLineTo(6f)
        quadTo(7f, 3.92f, 8.46f, 2.46f)
        reflectiveQuadTo(12f, 1f)
        reflectiveQuadToRelative(3.54f, 1.46f)
        reflectiveQuadTo(17f, 6f)
        verticalLineTo(8f)
        horizontalLineToRelative(1f)
        quadToRelative(0.82f, 0f, 1.41f, 0.59f)
        reflectiveQuadTo(20f, 10f)
        verticalLineTo(20f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(18f, 22f)
        horizontalLineTo(6f)
        close()
        moveToRelative(7.41f, -5.59f)
        quadTo(14f, 15.83f, 14f, 15f)
        reflectiveQuadTo(13.41f, 13.59f)
        reflectiveQuadTo(12f, 13f)
        reflectiveQuadToRelative(-1.41f, 0.59f)
        quadTo(10f, 14.18f, 10f, 15f)
        reflectiveQuadToRelative(0.59f, 1.41f)
        reflectiveQuadTo(12f, 17f)
        reflectiveQuadToRelative(1.41f, -0.59f)
        close()
        moveTo(9f, 8f)
        horizontalLineToRelative(6f)
        verticalLineTo(6f)
        quadTo(15f, 4.75f, 14.13f, 3.88f)
        reflectiveQuadTo(12f, 3f)
        reflectiveQuadTo(9.88f, 3.88f)
        reflectiveQuadTo(9f, 6f)
        verticalLineTo(8f)
        close()
    }
}
