// Adapted from Google Material Symbols (Rounded, notifications, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Notifications: ImageVector
    get() = notifications

private val notifications by lazy {
    materialSymbol("Rounded.Notifications") {
        moveTo(5f, 19f)
        quadTo(4.58f, 19f, 4.29f, 18.71f)
        quadTo(4f, 18.43f, 4f, 18f)
        reflectiveQuadTo(4.29f, 17.29f)
        reflectiveQuadTo(5f, 17f)
        horizontalLineTo(6f)
        verticalLineTo(10f)
        quadTo(6f, 7.93f, 7.25f, 6.31f)
        reflectiveQuadTo(10.5f, 4.2f)
        verticalLineTo(3.5f)
        quadToRelative(0f, -0.63f, 0.44f, -1.06f)
        reflectiveQuadTo(12f, 2f)
        reflectiveQuadToRelative(1.06f, 0.44f)
        reflectiveQuadTo(13.5f, 3.5f)
        verticalLineTo(4.2f)
        quadToRelative(2f, 0.5f, 3.25f, 2.11f)
        reflectiveQuadTo(18f, 10f)
        verticalLineToRelative(7f)
        horizontalLineToRelative(1f)
        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
        reflectiveQuadTo(20f, 18f)
        reflectiveQuadToRelative(-0.29f, 0.71f)
        reflectiveQuadTo(19f, 19f)
        horizontalLineTo(5f)
        close()
        moveToRelative(7f, 3f)
        quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
        reflectiveQuadTo(10f, 20f)
        horizontalLineToRelative(4f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(12f, 22f)
        close()
    }
}
