// Adapted from Google Material Symbols (Outlined, error, FILL=0).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.ErrorOutline: ImageVector
    get() = errorOutline

private val errorOutline by lazy {
    materialSymbol("Outlined.ErrorOutline") {
        moveTo(12.71f, 16.71f)
        quadTo(13f, 16.43f, 13f, 16f)
        reflectiveQuadTo(12.71f, 15.29f)
        reflectiveQuadTo(12f, 15f)
        reflectiveQuadToRelative(-0.71f, 0.29f)
        reflectiveQuadTo(11f, 16f)
        reflectiveQuadToRelative(0.29f, 0.71f)
        reflectiveQuadTo(12f, 17f)
        reflectiveQuadToRelative(0.71f, -0.29f)
        close()
        moveTo(11f, 13f)
        horizontalLineToRelative(2f)
        verticalLineTo(7f)
        horizontalLineTo(11f)
        verticalLineToRelative(6f)
        close()
        moveToRelative(1f, 9f)
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
        moveToRelative(0f, -2f)
        quadToRelative(3.35f, 0f, 5.68f, -2.32f)
        reflectiveQuadTo(20f, 12f)
        reflectiveQuadTo(17.68f, 6.32f)
        reflectiveQuadTo(12f, 4f)
        reflectiveQuadTo(6.33f, 6.32f)
        reflectiveQuadTo(4f, 12f)
        reflectiveQuadToRelative(2.33f, 5.68f)
        reflectiveQuadTo(12f, 20f)
        close()
        moveToRelative(0f, -8f)
        close()
    }
}
