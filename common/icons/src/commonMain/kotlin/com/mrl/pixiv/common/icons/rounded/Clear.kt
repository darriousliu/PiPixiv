// Adapted from Google Material Symbols (Rounded, clear, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Clear: ImageVector
    get() = clear

private val clear by lazy {
    materialSymbol("Rounded.Clear") {
        moveTo(12f, 13.4f)
        lineTo(7.1f, 18.3f)
        quadTo(6.83f, 18.58f, 6.4f, 18.58f)
        reflectiveQuadTo(5.7f, 18.3f)
        quadTo(5.43f, 18.02f, 5.43f, 17.6f)
        reflectiveQuadTo(5.7f, 16.9f)
        lineTo(10.6f, 12f)
        lineTo(5.7f, 7.1f)
        quadTo(5.43f, 6.82f, 5.43f, 6.4f)
        reflectiveQuadTo(5.7f, 5.7f)
        reflectiveQuadTo(6.4f, 5.43f)
        reflectiveQuadTo(7.1f, 5.7f)
        lineTo(12f, 10.6f)
        lineTo(16.9f, 5.7f)
        quadTo(17.18f, 5.43f, 17.6f, 5.43f)
        reflectiveQuadTo(18.3f, 5.7f)
        reflectiveQuadToRelative(0.27f, 0.7f)
        reflectiveQuadTo(18.3f, 7.1f)
        lineTo(13.4f, 12f)
        lineToRelative(4.9f, 4.9f)
        quadToRelative(0.27f, 0.28f, 0.27f, 0.7f)
        quadToRelative(0f, 0.42f, -0.27f, 0.7f)
        reflectiveQuadToRelative(-0.7f, 0.27f)
        reflectiveQuadTo(16.9f, 18.3f)
        lineTo(12f, 13.4f)
        close()
    }
}
