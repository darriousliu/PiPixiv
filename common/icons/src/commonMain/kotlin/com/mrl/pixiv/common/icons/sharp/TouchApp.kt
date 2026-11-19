// Adapted from Google Material Symbols (Sharp, touch_app, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.TouchApp: ImageVector
    get() = touchApp

private val touchApp by lazy {
    materialSymbol("Sharp.TouchApp") {
        moveTo(20f, 22f)
        horizontalLineTo(9.03f)
        lineTo(2.68f, 13.93f)
        lineToRelative(1.6f, -1.67f)
        lineTo(7.5f, 14.2f)
        verticalLineTo(5f)
        horizontalLineTo(9.53f)
        verticalLineToRelative(6f)
        horizontalLineTo(20f)
        verticalLineTo(22f)
        close()
        moveTo(4.18f, 8.5f)
        quadTo(3.85f, 7.95f, 3.68f, 7.31f)
        reflectiveQuadTo(3.5f, 6f)
        quadTo(3.5f, 3.92f, 4.96f, 2.46f)
        reflectiveQuadTo(8.5f, 1f)
        reflectiveQuadToRelative(3.54f, 1.46f)
        reflectiveQuadTo(13.5f, 6f)
        quadToRelative(0f, 0.68f, -0.17f, 1.31f)
        reflectiveQuadTo(12.83f, 8.5f)
        lineTo(11.1f, 7.5f)
        quadTo(11.3f, 7.15f, 11.4f, 6.79f)
        quadTo(11.5f, 6.43f, 11.5f, 6f)
        quadToRelative(0f, -1.25f, -0.88f, -2.13f)
        reflectiveQuadTo(8.5f, 3f)
        reflectiveQuadTo(6.38f, 3.88f)
        reflectiveQuadTo(5.5f, 6f)
        quadToRelative(0f, 0.43f, 0.1f, 0.79f)
        reflectiveQuadTo(5.9f, 7.5f)
        lineToRelative(-1.73f, 1f)
        close()
    }
}
