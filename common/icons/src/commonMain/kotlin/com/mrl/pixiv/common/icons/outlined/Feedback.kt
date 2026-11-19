// Adapted from Google Material Symbols (Outlined, feedback, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Feedback: ImageVector
    get() = feedback

private val feedback by lazy {
    materialSymbol("Outlined.Feedback") {
        moveTo(2f, 22f)
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
        lineTo(2f, 22f)
        close()
        moveTo(12f, 15f)
        quadToRelative(0.43f, 0f, 0.71f, -0.29f)
        reflectiveQuadTo(13f, 14f)
        reflectiveQuadTo(12.71f, 13.29f)
        reflectiveQuadTo(12f, 13f)
        reflectiveQuadToRelative(-0.71f, 0.29f)
        reflectiveQuadTo(11f, 14f)
        reflectiveQuadToRelative(0.29f, 0.71f)
        reflectiveQuadTo(12f, 15f)
        close()
        moveTo(11f, 11f)
        horizontalLineToRelative(2f)
        verticalLineTo(5f)
        horizontalLineTo(11f)
        verticalLineToRelative(6f)
        close()
    }
}
