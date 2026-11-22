// Adapted from Google Material Symbols (Rounded, expand_more, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.ExpandMore: ImageVector
    get() = expandMore

private val expandMore by lazy {
    materialSymbol("Rounded.ExpandMore") {
        moveTo(11.63f, 14.89f)
        quadTo(11.45f, 14.83f, 11.3f, 14.68f)
        lineTo(6.7f, 10.07f)
        quadTo(6.43f, 9.8f, 6.43f, 9.38f)
        reflectiveQuadTo(6.7f, 8.67f)
        reflectiveQuadTo(7.4f, 8.4f)
        reflectiveQuadTo(8.1f, 8.67f)
        lineToRelative(3.9f, 3.9f)
        lineToRelative(3.9f, -3.9f)
        quadTo(16.18f, 8.4f, 16.6f, 8.4f)
        reflectiveQuadToRelative(0.7f, 0.28f)
        reflectiveQuadToRelative(0.27f, 0.7f)
        reflectiveQuadToRelative(-0.27f, 0.7f)
        lineToRelative(-4.6f, 4.6f)
        quadToRelative(-0.15f, 0.15f, -0.33f, 0.21f)
        reflectiveQuadTo(12f, 14.95f)
        reflectiveQuadTo(11.63f, 14.89f)
        close()
    }
}
