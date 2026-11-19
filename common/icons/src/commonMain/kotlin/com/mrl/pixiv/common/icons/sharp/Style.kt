// Adapted from Google Material Symbols (Sharp, style, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Style: ImageVector
    get() = style

private val style by lazy {
    materialSymbol("Sharp.Style") {
        moveTo(3.98f, 19.8f)
        lineTo(1.28f, 18.7f)
        lineToRelative(2.7f, -5.85f)
        verticalLineTo(19.8f)
        close()
        moveToRelative(2f, -5.8f)
        lineToRelative(2.87f, 8f)
        horizontalLineTo(5.98f)
        verticalLineTo(14f)
        close()
        moveToRelative(5.25f, 8.6f)
        lineTo(5.38f, 6.55f)
        lineTo(16.73f, 2.4f)
        lineToRelative(5.85f, 16.05f)
        lineTo(11.23f, 22.6f)
        close()
        moveTo(11.69f, 9.71f)
        quadTo(11.98f, 9.42f, 11.98f, 9f)
        quadToRelative(0f, -0.43f, -0.29f, -0.71f)
        reflectiveQuadTo(10.98f, 8f)
        reflectiveQuadTo(10.26f, 8.29f)
        reflectiveQuadTo(9.98f, 9f)
        quadToRelative(0f, 0.42f, 0.29f, 0.71f)
        reflectiveQuadTo(10.98f, 10f)
        reflectiveQuadTo(11.69f, 9.71f)
        close()
    }
}
