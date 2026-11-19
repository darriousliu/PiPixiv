// Adapted from Google Material Symbols (Rounded, file_copy, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.FileCopy: ImageVector
    get() = fileCopy

private val fileCopy by lazy {
    materialSymbol("Rounded.FileCopy") {
        moveTo(19f, 19f)
        horizontalLineTo(8f)
        quadTo(7.18f, 19f, 6.59f, 18.41f)
        reflectiveQuadTo(6f, 17f)
        verticalLineTo(3f)
        quadTo(6f, 2.17f, 6.59f, 1.59f)
        reflectiveQuadTo(8f, 1f)
        horizontalLineToRelative(6.18f)
        quadToRelative(0.4f, 0f, 0.76f, 0.15f)
        reflectiveQuadToRelative(0.64f, 0.43f)
        lineToRelative(4.85f, 4.85f)
        quadTo(20.7f, 6.7f, 20.85f, 7.06f)
        reflectiveQuadTo(21f, 7.82f)
        verticalLineTo(17f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(19f, 19f)
        close()
        moveTo(19f, 8f)
        lineTo(14f, 3f)
        verticalLineTo(6.5f)
        quadToRelative(0f, 0.63f, 0.44f, 1.06f)
        reflectiveQuadTo(15.5f, 8f)
        horizontalLineTo(19f)
        close()
        moveTo(4f, 23f)
        quadTo(3.18f, 23f, 2.59f, 22.41f)
        reflectiveQuadTo(2f, 21f)
        verticalLineTo(8f)
        quadTo(2f, 7.57f, 2.29f, 7.29f)
        reflectiveQuadTo(3f, 7f)
        reflectiveQuadTo(3.71f, 7.29f)
        reflectiveQuadTo(4f, 8f)
        verticalLineTo(21f)
        horizontalLineTo(14f)
        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
        reflectiveQuadTo(15f, 22f)
        reflectiveQuadToRelative(-0.29f, 0.71f)
        reflectiveQuadTo(14f, 23f)
        horizontalLineTo(4f)
        close()
    }
}
