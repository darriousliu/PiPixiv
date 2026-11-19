// Adapted from Google Material Symbols (Sharp, content_paste, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.ContentPaste: ImageVector
    get() = contentPaste

private val contentPaste by lazy {
    materialSymbol("Sharp.ContentPaste") {
        moveTo(3f, 21f)
        verticalLineTo(3f)
        horizontalLineTo(9.18f)
        quadTo(9.45f, 2.13f, 10.25f, 1.56f)
        reflectiveQuadTo(12f, 1f)
        quadToRelative(1f, 0f, 1.79f, 0.56f)
        reflectiveQuadTo(14.85f, 3f)
        horizontalLineTo(21f)
        verticalLineTo(21f)
        horizontalLineTo(3f)
        close()
        moveTo(5f, 19f)
        horizontalLineTo(19f)
        verticalLineTo(5f)
        horizontalLineTo(17f)
        verticalLineTo(8f)
        horizontalLineTo(7f)
        verticalLineTo(5f)
        horizontalLineTo(5f)
        verticalLineTo(19f)
        close()
        moveTo(12.71f, 4.71f)
        quadTo(13f, 4.42f, 13f, 4f)
        quadTo(13f, 3.57f, 12.71f, 3.29f)
        reflectiveQuadTo(12f, 3f)
        reflectiveQuadTo(11.29f, 3.29f)
        reflectiveQuadTo(11f, 4f)
        quadToRelative(0f, 0.42f, 0.29f, 0.71f)
        reflectiveQuadTo(12f, 5f)
        reflectiveQuadTo(12.71f, 4.71f)
        close()
    }
}
