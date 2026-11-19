// Adapted from Google Material Symbols (Rounded, arrow_back_ios_new, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.ArrowBackIos: ImageVector
    get() = arrowBackIos

private val arrowBackIos by lazy {
    materialSymbol("Rounded.ArrowBackIos", autoMirror = true) {
        moveTo(9.55f, 12f)
        lineToRelative(7.35f, 7.35f)
        quadToRelative(0.38f, 0.38f, 0.36f, 0.88f)
        reflectiveQuadTo(16.88f, 21.1f)
        reflectiveQuadTo(16f, 21.48f)
        reflectiveQuadTo(15.13f, 21.1f)
        lineTo(7.43f, 13.43f)
        quadTo(7.13f, 13.13f, 6.98f, 12.75f)
        reflectiveQuadTo(6.83f, 12f)
        reflectiveQuadTo(6.98f, 11.25f)
        reflectiveQuadTo(7.43f, 10.58f)
        lineToRelative(7.7f, -7.7f)
        quadTo(15.5f, 2.5f, 16.01f, 2.51f)
        quadTo(16.53f, 2.52f, 16.9f, 2.9f)
        reflectiveQuadToRelative(0.38f, 0.88f)
        reflectiveQuadTo(16.9f, 4.65f)
        lineTo(9.55f, 12f)
        close()
    }
}
