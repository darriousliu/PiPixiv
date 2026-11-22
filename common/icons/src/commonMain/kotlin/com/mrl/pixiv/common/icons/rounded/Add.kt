// Adapted from Google Material Symbols (Rounded, add, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Add: ImageVector
    get() = add

private val add by lazy {
    materialSymbol("Rounded.Add") {
        moveTo(11f, 13f)
        horizontalLineTo(6f)
        quadTo(5.58f, 13f, 5.29f, 12.71f)
        quadTo(5f, 12.43f, 5f, 12f)
        reflectiveQuadTo(5.29f, 11.29f)
        reflectiveQuadTo(6f, 11f)
        horizontalLineToRelative(5f)
        verticalLineTo(6f)
        quadTo(11f, 5.57f, 11.29f, 5.29f)
        reflectiveQuadTo(12f, 5f)
        reflectiveQuadToRelative(0.71f, 0.29f)
        reflectiveQuadTo(13f, 6f)
        verticalLineToRelative(5f)
        horizontalLineToRelative(5f)
        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
        reflectiveQuadTo(19f, 12f)
        reflectiveQuadToRelative(-0.29f, 0.71f)
        reflectiveQuadTo(18f, 13f)
        horizontalLineTo(13f)
        verticalLineToRelative(5f)
        quadToRelative(0f, 0.43f, -0.29f, 0.71f)
        reflectiveQuadTo(12f, 19f)
        reflectiveQuadTo(11.29f, 18.71f)
        quadTo(11f, 18.43f, 11f, 18f)
        verticalLineTo(13f)
        close()
    }
}
