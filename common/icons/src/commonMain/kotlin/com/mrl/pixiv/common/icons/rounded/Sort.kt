// Adapted from Google Material Symbols (Rounded, sort, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Sort: ImageVector
    get() = sort

private val sort by lazy {
    materialSymbol("Rounded.Sort", autoMirror = true) {
        moveTo(4f, 18f)
        quadTo(3.58f, 18f, 3.29f, 17.71f)
        quadTo(3f, 17.43f, 3f, 17f)
        reflectiveQuadTo(3.29f, 16.29f)
        reflectiveQuadTo(4f, 16f)
        horizontalLineTo(8f)
        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
        reflectiveQuadTo(9f, 17f)
        reflectiveQuadTo(8.71f, 17.71f)
        reflectiveQuadTo(8f, 18f)
        horizontalLineTo(4f)
        close()
        moveTo(4f, 13f)
        quadTo(3.58f, 13f, 3.29f, 12.71f)
        quadTo(3f, 12.43f, 3f, 12f)
        reflectiveQuadTo(3.29f, 11.29f)
        reflectiveQuadTo(4f, 11f)
        horizontalLineTo(14f)
        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
        reflectiveQuadTo(15f, 12f)
        reflectiveQuadToRelative(-0.29f, 0.71f)
        reflectiveQuadTo(14f, 13f)
        horizontalLineTo(4f)
        close()
        moveTo(4f, 8f)
        quadTo(3.58f, 8f, 3.29f, 7.71f)
        quadTo(3f, 7.43f, 3f, 7f)
        reflectiveQuadTo(3.29f, 6.29f)
        reflectiveQuadTo(4f, 6f)
        horizontalLineTo(20f)
        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
        reflectiveQuadTo(21f, 7f)
        reflectiveQuadTo(20.71f, 7.71f)
        reflectiveQuadTo(20f, 8f)
        horizontalLineTo(4f)
        close()
    }
}
