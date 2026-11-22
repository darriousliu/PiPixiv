// Adapted from Google Material Symbols (Rounded, collections, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Collections: ImageVector
    get() = collections

private val collections by lazy {
    materialSymbol("Rounded.Collections") {
        moveTo(13.25f, 12.5f)
        lineTo(12.1f, 11f)
        quadTo(11.95f, 10.8f, 11.7f, 10.8f)
        reflectiveQuadTo(11.3f, 11f)
        lineTo(9.63f, 13.2f)
        quadToRelative(-0.2f, 0.25f, -0.06f, 0.53f)
        reflectiveQuadTo(10.03f, 14f)
        horizontalLineToRelative(7.95f)
        quadToRelative(0.32f, 0f, 0.46f, -0.28f)
        reflectiveQuadTo(18.38f, 13.2f)
        lineTo(15.95f, 10.02f)
        quadTo(15.8f, 9.82f, 15.55f, 9.82f)
        reflectiveQuadToRelative(-0.4f, 0.2f)
        lineToRelative(-1.9f, 2.48f)
        close()
        moveTo(8f, 18f)
        quadTo(7.18f, 18f, 6.59f, 17.41f)
        reflectiveQuadTo(6f, 16f)
        verticalLineTo(4f)
        quadTo(6f, 3.17f, 6.59f, 2.59f)
        reflectiveQuadTo(8f, 2f)
        horizontalLineTo(20f)
        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
        reflectiveQuadTo(22f, 4f)
        verticalLineTo(16f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(20f, 18f)
        horizontalLineTo(8f)
        close()
        moveTo(4f, 22f)
        quadTo(3.18f, 22f, 2.59f, 21.41f)
        reflectiveQuadTo(2f, 20f)
        verticalLineTo(7f)
        quadTo(2f, 6.57f, 2.29f, 6.29f)
        reflectiveQuadTo(3f, 6f)
        reflectiveQuadTo(3.71f, 6.29f)
        reflectiveQuadTo(4f, 7f)
        verticalLineTo(20f)
        horizontalLineTo(17f)
        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
        reflectiveQuadTo(18f, 21f)
        reflectiveQuadToRelative(-0.29f, 0.71f)
        reflectiveQuadTo(17f, 22f)
        horizontalLineTo(4f)
        close()
    }
}
