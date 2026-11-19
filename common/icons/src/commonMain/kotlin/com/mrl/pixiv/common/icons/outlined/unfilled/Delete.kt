// Adapted from Google Material Symbols (Outlined, delete, FILL=0).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined.unfilled

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Delete: ImageVector
    get() = delete

private val delete by lazy {
    materialSymbol("Outlined.Delete.0") {
        moveTo(7f, 21f)
        quadTo(6.18f, 21f, 5.59f, 20.41f)
        reflectiveQuadTo(5f, 19f)
        verticalLineTo(6f)
        horizontalLineTo(4f)
        verticalLineTo(4f)
        horizontalLineTo(9f)
        verticalLineTo(3f)
        horizontalLineToRelative(6f)
        verticalLineTo(4f)
        horizontalLineToRelative(5f)
        verticalLineTo(6f)
        horizontalLineTo(19f)
        verticalLineTo(19f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(17f, 21f)
        horizontalLineTo(7f)
        close()
        moveTo(17f, 6f)
        horizontalLineTo(7f)
        verticalLineTo(19f)
        horizontalLineTo(17f)
        verticalLineTo(6f)
        close()
        moveTo(9f, 17f)
        horizontalLineToRelative(2f)
        verticalLineTo(8f)
        horizontalLineTo(9f)
        verticalLineToRelative(9f)
        close()
        moveToRelative(4f, 0f)
        horizontalLineToRelative(2f)
        verticalLineTo(8f)
        horizontalLineTo(13f)
        verticalLineToRelative(9f)
        close()
        moveTo(7f, 6f)
        verticalLineTo(19f)
        verticalLineTo(6f)
        close()
    }
}
