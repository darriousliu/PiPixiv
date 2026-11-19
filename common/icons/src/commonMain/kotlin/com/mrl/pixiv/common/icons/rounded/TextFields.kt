// Adapted from Google Material Symbols (Rounded, text_fields, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.TextFields: ImageVector
    get() = textFields

private val textFields by lazy {
    materialSymbol("Rounded.TextFields") {
        moveTo(7.44f, 19.56f)
        quadTo(7f, 19.13f, 7f, 18.5f)
        verticalLineTo(7f)
        horizontalLineTo(3.5f)
        quadTo(2.88f, 7f, 2.44f, 6.56f)
        reflectiveQuadTo(2f, 5.5f)
        reflectiveQuadTo(2.44f, 4.44f)
        reflectiveQuadTo(3.5f, 4f)
        horizontalLineToRelative(10f)
        quadToRelative(0.63f, 0f, 1.06f, 0.44f)
        reflectiveQuadTo(15f, 5.5f)
        reflectiveQuadTo(14.56f, 6.56f)
        reflectiveQuadTo(13.5f, 7f)
        horizontalLineTo(10f)
        verticalLineTo(18.5f)
        quadToRelative(0f, 0.63f, -0.44f, 1.06f)
        reflectiveQuadTo(8.5f, 20f)
        reflectiveQuadTo(7.44f, 19.56f)
        close()
        moveToRelative(9f, 0f)
        quadTo(16f, 19.13f, 16f, 18.5f)
        verticalLineTo(12f)
        horizontalLineTo(14.5f)
        quadToRelative(-0.63f, 0f, -1.06f, -0.44f)
        reflectiveQuadTo(13f, 10.5f)
        reflectiveQuadTo(13.44f, 9.44f)
        reflectiveQuadTo(14.5f, 9f)
        horizontalLineToRelative(6f)
        quadToRelative(0.63f, 0f, 1.06f, 0.44f)
        reflectiveQuadTo(22f, 10.5f)
        reflectiveQuadToRelative(-0.44f, 1.06f)
        reflectiveQuadTo(20.5f, 12f)
        horizontalLineTo(19f)
        verticalLineToRelative(6.5f)
        quadToRelative(0f, 0.63f, -0.44f, 1.06f)
        reflectiveQuadTo(17.5f, 20f)
        reflectiveQuadTo(16.44f, 19.56f)
        close()
    }
}
