// Adapted from Google Material Symbols (Outlined, view_list, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.ViewList: ImageVector
    get() = viewList

private val viewList by lazy {
    materialSymbol("Outlined.ViewList", autoMirror = true) {
        moveTo(9f, 20f)
        horizontalLineTo(20f)
        quadToRelative(0.83f, 0f, 1.41f, -0.59f)
        reflectiveQuadTo(22f, 18f)
        verticalLineTo(16f)
        horizontalLineTo(9f)
        verticalLineToRelative(4f)
        close()
        moveTo(2f, 8f)
        horizontalLineTo(7f)
        verticalLineTo(4f)
        horizontalLineTo(4f)
        quadTo(3.18f, 4f, 2.59f, 4.59f)
        quadTo(2f, 5.18f, 2f, 6f)
        verticalLineTo(8f)
        close()
        moveToRelative(0f, 6f)
        horizontalLineTo(7f)
        verticalLineTo(10f)
        horizontalLineTo(2f)
        verticalLineToRelative(4f)
        close()
        moveToRelative(2f, 6f)
        horizontalLineTo(7f)
        verticalLineTo(16f)
        horizontalLineTo(2f)
        verticalLineToRelative(2f)
        quadToRelative(0f, 0.82f, 0.59f, 1.41f)
        reflectiveQuadTo(4f, 20f)
        close()
        moveTo(9f, 14f)
        horizontalLineTo(22f)
        verticalLineTo(10f)
        horizontalLineTo(9f)
        verticalLineToRelative(4f)
        close()
        moveTo(9f, 8f)
        horizontalLineTo(22f)
        verticalLineTo(6f)
        quadTo(22f, 5.18f, 21.41f, 4.59f)
        reflectiveQuadTo(20f, 4f)
        horizontalLineTo(9f)
        verticalLineTo(8f)
        close()
    }
}
