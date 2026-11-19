// Adapted from Google Material Symbols (Outlined, image, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Image: ImageVector
    get() = image

private val image by lazy {
    materialSymbol("Outlined.Image") {
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
        moveTo(6f, 17f)
        horizontalLineTo(18f)
        lineTo(14.25f, 12f)
        lineToRelative(-3f, 4f)
        lineTo(9f, 13f)
        lineTo(6f, 17f)
        close()
    }
}
