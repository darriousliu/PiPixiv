// Adapted from Google Material Symbols (Outlined, download, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Download: ImageVector
    get() = download

private val download by lazy {
    materialSymbol("Outlined.Download") {
        moveTo(12f, 16f)
        lineTo(7f, 11f)
        lineTo(8.4f, 9.55f)
        lineToRelative(2.6f, 2.6f)
        verticalLineTo(4f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(8.15f)
        lineToRelative(2.6f, -2.6f)
        lineTo(17f, 11f)
        lineToRelative(-5f, 5f)
        close()
        moveTo(6f, 20f)
        quadTo(5.18f, 20f, 4.59f, 19.41f)
        reflectiveQuadTo(4f, 18f)
        verticalLineTo(15f)
        horizontalLineTo(6f)
        verticalLineToRelative(3f)
        horizontalLineTo(18f)
        verticalLineTo(15f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(3f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(18f, 20f)
        horizontalLineTo(6f)
        close()
    }
}
