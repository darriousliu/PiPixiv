// Adapted from Google Material Symbols (Outlined, folder, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Folder: ImageVector
    get() = folder

private val folder by lazy {
    materialSymbol("Outlined.Folder") {
        moveTo(4f, 20f)
        quadTo(3.18f, 20f, 2.59f, 19.41f)
        reflectiveQuadTo(2f, 18f)
        verticalLineTo(6f)
        quadTo(2f, 5.18f, 2.59f, 4.59f)
        reflectiveQuadTo(4f, 4f)
        horizontalLineToRelative(6f)
        lineToRelative(2f, 2f)
        horizontalLineToRelative(8f)
        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
        quadTo(22f, 7.18f, 22f, 8f)
        verticalLineTo(18f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(20f, 20f)
        horizontalLineTo(4f)
        close()
    }
}
