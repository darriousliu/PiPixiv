// Adapted from Google Material Symbols (Rounded, bookmark, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Bookmark: ImageVector
    get() = bookmark

private val bookmark by lazy {
    materialSymbol("Rounded.Bookmark") {
        moveTo(12f, 18f)
        lineTo(7.8f, 19.8f)
        quadToRelative(-1f, 0.43f, -1.9f, -0.16f)
        reflectiveQuadTo(5f, 17.98f)
        verticalLineTo(5f)
        quadTo(5f, 4.17f, 5.59f, 3.59f)
        reflectiveQuadTo(7f, 3f)
        horizontalLineTo(17f)
        quadToRelative(0.82f, 0f, 1.41f, 0.59f)
        reflectiveQuadTo(19f, 5f)
        verticalLineTo(17.98f)
        quadToRelative(0f, 1.07f, -0.9f, 1.66f)
        quadToRelative(-0.9f, 0.59f, -1.9f, 0.16f)
        lineTo(12f, 18f)
        close()
    }
}
