// Adapted from Google Material Symbols (Outlined, filter_alt, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.FilterAlt: ImageVector
    get() = filterAlt

private val filterAlt by lazy {
    materialSymbol("Outlined.FilterAlt") {
        moveTo(11f, 20f)
        quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
        quadTo(10f, 19.43f, 10f, 19f)
        verticalLineTo(13f)
        lineTo(4.2f, 5.6f)
        quadTo(3.83f, 5.1f, 4.09f, 4.55f)
        reflectiveQuadTo(5f, 4f)
        horizontalLineTo(19f)
        quadToRelative(0.65f, 0f, 0.91f, 0.55f)
        reflectiveQuadTo(19.8f, 5.6f)
        lineTo(14f, 13f)
        verticalLineToRelative(6f)
        quadToRelative(0f, 0.43f, -0.29f, 0.71f)
        reflectiveQuadTo(13f, 20f)
        horizontalLineTo(11f)
        close()
    }
}
