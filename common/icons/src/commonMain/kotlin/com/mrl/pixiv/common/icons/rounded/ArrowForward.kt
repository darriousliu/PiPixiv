// Adapted from Google Material Symbols (Rounded, arrow_forward, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.ArrowForward: ImageVector
    get() = arrowForward

private val arrowForward by lazy {
    materialSymbol("Rounded.ArrowForward", autoMirror = true) {
        moveTo(16.18f, 13f)
        horizontalLineTo(5f)
        quadTo(4.58f, 13f, 4.29f, 12.71f)
        quadTo(4f, 12.43f, 4f, 12f)
        reflectiveQuadTo(4.29f, 11.29f)
        reflectiveQuadTo(5f, 11f)
        horizontalLineTo(16.18f)
        lineTo(11.28f, 6.1f)
        quadTo(10.98f, 5.8f, 10.99f, 5.4f)
        reflectiveQuadTo(11.3f, 4.7f)
        quadTo(11.6f, 4.42f, 12f, 4.41f)
        reflectiveQuadTo(12.7f, 4.7f)
        lineToRelative(6.6f, 6.6f)
        quadToRelative(0.15f, 0.15f, 0.21f, 0.33f)
        reflectiveQuadTo(19.58f, 12f)
        reflectiveQuadToRelative(-0.06f, 0.38f)
        reflectiveQuadTo(19.3f, 12.7f)
        lineToRelative(-6.6f, 6.6f)
        quadToRelative(-0.28f, 0.27f, -0.69f, 0.27f)
        reflectiveQuadTo(11.3f, 19.3f)
        quadTo(11f, 19f, 11f, 18.59f)
        quadToRelative(0f, -0.41f, 0.3f, -0.71f)
        lineTo(16.18f, 13f)
        close()
    }
}
