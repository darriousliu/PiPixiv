// Adapted from Google Material Symbols (Rounded, feedback, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Feedback: ImageVector
    get() = feedback

private val feedback by lazy {
    materialSymbol("Rounded.Feedback") {
        moveTo(6f, 18f)
        lineTo(3.7f, 20.3f)
        quadTo(3.23f, 20.78f, 2.61f, 20.51f)
        reflectiveQuadTo(2f, 19.58f)
        verticalLineTo(4f)
        quadTo(2f, 3.17f, 2.59f, 2.59f)
        reflectiveQuadTo(4f, 2f)
        horizontalLineTo(20f)
        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
        reflectiveQuadTo(22f, 4f)
        verticalLineTo(16f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(20f, 18f)
        horizontalLineTo(6f)
        close()
        moveToRelative(6f, -3f)
        quadToRelative(0.43f, 0f, 0.71f, -0.29f)
        reflectiveQuadTo(13f, 14f)
        reflectiveQuadTo(12.71f, 13.29f)
        reflectiveQuadTo(12f, 13f)
        reflectiveQuadToRelative(-0.71f, 0.29f)
        reflectiveQuadTo(11f, 14f)
        reflectiveQuadToRelative(0.29f, 0.71f)
        reflectiveQuadTo(12f, 15f)
        close()
        moveToRelative(0.71f, -4.29f)
        quadTo(13f, 10.43f, 13f, 10f)
        verticalLineTo(6f)
        quadTo(13f, 5.57f, 12.71f, 5.29f)
        reflectiveQuadTo(12f, 5f)
        reflectiveQuadTo(11.29f, 5.29f)
        reflectiveQuadTo(11f, 6f)
        verticalLineToRelative(4f)
        quadToRelative(0f, 0.42f, 0.29f, 0.71f)
        reflectiveQuadTo(12f, 11f)
        reflectiveQuadToRelative(0.71f, -0.29f)
        close()
    }
}
