// Adapted from Google Material Symbols (Outlined, file_copy, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.FileCopy: ImageVector
    get() = fileCopy

private val fileCopy by lazy {
    materialSymbol("Outlined.FileCopy") {
        moveTo(4f, 23f)
        quadTo(3.18f, 23f, 2.59f, 22.41f)
        reflectiveQuadTo(2f, 21f)
        verticalLineTo(7f)
        horizontalLineTo(4f)
        verticalLineTo(21f)
        horizontalLineTo(15f)
        verticalLineToRelative(2f)
        horizontalLineTo(4f)
        close()
        moveTo(8f, 19f)
        quadTo(7.18f, 19f, 6.59f, 18.41f)
        reflectiveQuadTo(6f, 17f)
        verticalLineTo(3f)
        quadTo(6f, 2.17f, 6.59f, 1.59f)
        reflectiveQuadTo(8f, 1f)
        horizontalLineToRelative(7f)
        lineToRelative(6f, 6f)
        verticalLineTo(17f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(19f, 19f)
        horizontalLineTo(8f)
        close()
        moveTo(14f, 8f)
        horizontalLineToRelative(5f)
        lineTo(14f, 3f)
        verticalLineTo(8f)
        close()
    }
}
