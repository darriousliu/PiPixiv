// Adapted from Google Material Symbols (Outlined, hide_image, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.HideImage: ImageVector
    get() = hideImage

private val hideImage by lazy {
    materialSymbol("Outlined.HideImage") {
        moveTo(21f, 18.15f)
        lineTo(5.85f, 3f)
        horizontalLineTo(19f)
        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
        reflectiveQuadTo(21f, 5f)
        verticalLineTo(18.15f)
        close()
        moveTo(19.8f, 22.6f)
        lineTo(18.2f, 21f)
        horizontalLineTo(5f)
        quadTo(4.18f, 21f, 3.59f, 20.41f)
        reflectiveQuadTo(3f, 19f)
        verticalLineTo(5.8f)
        lineTo(1.4f, 4.2f)
        lineTo(2.8f, 2.8f)
        lineTo(21.2f, 21.2f)
        lineToRelative(-1.4f, 1.4f)
        close()
        moveTo(6f, 17f)
        horizontalLineToRelative(8.18f)
        lineToRelative(-2.1f, -2.1f)
        lineTo(11.25f, 16f)
        lineTo(9f, 13f)
        lineTo(6f, 17f)
        close()
    }
}
