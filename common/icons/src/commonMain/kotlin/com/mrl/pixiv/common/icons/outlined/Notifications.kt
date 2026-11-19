// Adapted from Google Material Symbols (Outlined, notifications, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Notifications: ImageVector
    get() = notifications

private val notifications by lazy {
    materialSymbol("Outlined.Notifications") {
        moveTo(4f, 19f)
        verticalLineTo(17f)
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
        horizontalLineToRelative(2f)
        verticalLineToRelative(2f)
        horizontalLineTo(4f)
        close()
        moveToRelative(8f, 3f)
        quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
        reflectiveQuadTo(10f, 20f)
        horizontalLineToRelative(4f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(12f, 22f)
        close()
    }
}
