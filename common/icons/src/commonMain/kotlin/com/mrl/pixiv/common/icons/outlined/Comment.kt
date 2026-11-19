// Adapted from Google Material Symbols (Outlined, comment, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Comment: ImageVector
    get() = comment

private val comment by lazy {
    materialSymbol("Outlined.Comment", autoMirror = true) {
        moveTo(6f, 14f)
        horizontalLineTo(18f)
        verticalLineTo(12f)
        horizontalLineTo(6f)
        verticalLineToRelative(2f)
        close()
        moveTo(6f, 11f)
        horizontalLineTo(18f)
        verticalLineTo(9f)
        horizontalLineTo(6f)
        verticalLineToRelative(2f)
        close()
        moveTo(6f, 8f)
        horizontalLineTo(18f)
        verticalLineTo(6f)
        horizontalLineTo(6f)
        verticalLineTo(8f)
        close()
        moveTo(4f, 18f)
        quadTo(3.18f, 18f, 2.59f, 17.41f)
        reflectiveQuadTo(2f, 16f)
        verticalLineTo(4f)
        quadTo(2f, 3.17f, 2.59f, 2.59f)
        reflectiveQuadTo(4f, 2f)
        horizontalLineTo(20f)
        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
        reflectiveQuadTo(22f, 4f)
        verticalLineTo(22f)
        lineTo(18f, 18f)
        horizontalLineTo(4f)
        close()
    }
}
