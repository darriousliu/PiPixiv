// Adapted from Google Material Symbols (Rounded, equalizer, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Equalizer: ImageVector
    get() = equalizer

private val equalizer by lazy {
    materialSymbol("Rounded.Equalizer") {
        moveTo(4.59f, 19.41f)
        quadTo(4f, 18.83f, 4f, 18f)
        verticalLineTo(14f)
        quadTo(4f, 13.18f, 4.59f, 12.59f)
        reflectiveQuadTo(6f, 12f)
        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
        quadTo(8f, 13.18f, 8f, 14f)
        verticalLineToRelative(4f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(6f, 20f)
        reflectiveQuadTo(4.59f, 19.41f)
        close()
        moveTo(12f, 20f)
        quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
        reflectiveQuadTo(10f, 18f)
        verticalLineTo(6f)
        quadTo(10f, 5.18f, 10.59f, 4.59f)
        reflectiveQuadTo(12f, 4f)
        reflectiveQuadToRelative(1.41f, 0.59f)
        quadTo(14f, 5.18f, 14f, 6f)
        verticalLineTo(18f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(12f, 20f)
        close()
        moveToRelative(4.59f, -0.59f)
        quadTo(16f, 18.83f, 16f, 18f)
        verticalLineTo(11f)
        quadToRelative(0f, -0.83f, 0.59f, -1.41f)
        reflectiveQuadTo(18f, 9f)
        reflectiveQuadToRelative(1.41f, 0.59f)
        reflectiveQuadTo(20f, 11f)
        verticalLineToRelative(7f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(18f, 20f)
        reflectiveQuadTo(16.59f, 19.41f)
        close()
    }
}
