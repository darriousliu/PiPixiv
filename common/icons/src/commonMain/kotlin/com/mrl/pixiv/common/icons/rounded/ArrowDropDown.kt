// Adapted from Google Material Symbols (Rounded, arrow_drop_down, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.ArrowDropDown: ImageVector
    get() = arrowDropDown

private val arrowDropDown by lazy {
    materialSymbol("Rounded.ArrowDropDown") {
        moveTo(11.48f, 14.48f)
        lineTo(7.85f, 10.85f)
        quadTo(7.78f, 10.77f, 7.74f, 10.69f)
        reflectiveQuadTo(7.7f, 10.5f)
        quadToRelative(0f, -0.2f, 0.14f, -0.35f)
        reflectiveQuadTo(8.2f, 10f)
        horizontalLineToRelative(7.6f)
        quadToRelative(0.23f, 0f, 0.36f, 0.15f)
        reflectiveQuadTo(16.3f, 10.5f)
        quadToRelative(0f, 0.05f, -0.15f, 0.35f)
        lineToRelative(-3.63f, 3.63f)
        quadTo(12.4f, 14.6f, 12.28f, 14.65f)
        reflectiveQuadTo(12f, 14.7f)
        reflectiveQuadTo(11.73f, 14.65f)
        reflectiveQuadTo(11.48f, 14.48f)
        close()
    }
}
