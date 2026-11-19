// Adapted from Google Material Symbols (Rounded, arrow_back, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.ArrowBack: ImageVector
    get() = arrowBack

private val arrowBack by lazy {
    materialSymbol("Rounded.ArrowBack", autoMirror = true) {
        moveTo(7.83f, 13f)
        lineToRelative(4.9f, 4.9f)
        quadToRelative(0.3f, 0.3f, 0.29f, 0.7f)
        reflectiveQuadTo(12.7f, 19.3f)
        quadTo(12.4f, 19.58f, 12f, 19.59f)
        reflectiveQuadTo(11.3f, 19.3f)
        lineTo(4.7f, 12.7f)
        quadTo(4.55f, 12.55f, 4.49f, 12.38f)
        reflectiveQuadTo(4.43f, 12f)
        reflectiveQuadTo(4.49f, 11.63f)
        reflectiveQuadTo(4.7f, 11.3f)
        lineTo(11.3f, 4.7f)
        quadTo(11.58f, 4.42f, 11.99f, 4.42f)
        reflectiveQuadTo(12.7f, 4.7f)
        quadTo(13f, 5f, 13f, 5.41f)
        reflectiveQuadTo(12.7f, 6.13f)
        lineTo(7.83f, 11f)
        horizontalLineTo(19f)
        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
        reflectiveQuadTo(20f, 12f)
        reflectiveQuadToRelative(-0.29f, 0.71f)
        reflectiveQuadTo(19f, 13f)
        horizontalLineTo(7.83f)
        close()
    }
}
