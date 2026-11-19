// Adapted from Google Material Symbols (Outlined, 18_up_rating, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined._18UpRating: ImageVector
    get() = rating18Up

private val rating18Up by lazy {
    materialSymbol("Outlined._18UpRating") {
        moveTo(8.5f, 15f)
        horizontalLineTo(10f)
        verticalLineTo(9f)
        horizontalLineTo(7f)
        verticalLineToRelative(1.5f)
        horizontalLineTo(8.5f)
        verticalLineTo(15f)
        close()
        moveToRelative(4f, 0f)
        horizontalLineTo(15f)
        quadToRelative(0.43f, 0f, 0.71f, -0.29f)
        reflectiveQuadTo(16f, 14f)
        verticalLineTo(10f)
        quadTo(16f, 9.57f, 15.71f, 9.29f)
        reflectiveQuadTo(15f, 9f)
        horizontalLineTo(12.5f)
        quadTo(12.08f, 9f, 11.79f, 9.29f)
        reflectiveQuadTo(11.5f, 10f)
        verticalLineToRelative(4f)
        quadToRelative(0f, 0.42f, 0.29f, 0.71f)
        reflectiveQuadTo(12.5f, 15f)
        close()
        moveTo(13f, 14f)
        verticalLineTo(12.5f)
        horizontalLineToRelative(1.5f)
        verticalLineTo(14f)
        horizontalLineTo(13f)
        close()
        moveToRelative(0f, -2.5f)
        verticalLineTo(10f)
        horizontalLineToRelative(1.5f)
        verticalLineToRelative(1.5f)
        horizontalLineTo(13f)
        close()
        moveTo(5f, 21f)
        quadTo(4.18f, 21f, 3.59f, 20.41f)
        reflectiveQuadTo(3f, 19f)
        verticalLineTo(5f)
        quadTo(3f, 4.17f, 3.59f, 3.59f)
        reflectiveQuadTo(5f, 3f)
        horizontalLineTo(19f)
        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
        reflectiveQuadTo(21f, 5f)
        verticalLineTo(19f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(19f, 21f)
        horizontalLineTo(5f)
        close()
    }
}
