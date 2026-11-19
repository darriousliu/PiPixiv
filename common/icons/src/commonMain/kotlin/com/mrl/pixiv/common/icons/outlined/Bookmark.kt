// Adapted from Google Material Symbols (Outlined, bookmark, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Bookmark: ImageVector
    get() = bookmark

private val bookmark by lazy {
    materialSymbol("Outlined.Bookmark") {
        moveTo(5f, 21f)
        verticalLineTo(5f)
        quadTo(5f, 4.17f, 5.59f, 3.59f)
        reflectiveQuadTo(7f, 3f)
        horizontalLineTo(17f)
        quadToRelative(0.82f, 0f, 1.41f, 0.59f)
        reflectiveQuadTo(19f, 5f)
        verticalLineTo(21f)
        lineTo(12f, 18f)
        lineTo(5f, 21f)
        close()
    }
}
