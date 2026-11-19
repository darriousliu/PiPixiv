// Adapted from Google Material Symbols (Sharp, photo, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Photo: ImageVector
    get() = photo

private val photo by lazy {
    materialSymbol("Sharp.Photo") {
        moveTo(6f, 17f)
        horizontalLineTo(18f)
        lineTo(14.25f, 12f)
        lineToRelative(-3f, 4f)
        lineTo(9f, 13f)
        lineTo(6f, 17f)
        close()
        moveTo(3f, 21f)
        verticalLineTo(3f)
        horizontalLineTo(21f)
        verticalLineTo(21f)
        horizontalLineTo(3f)
        close()
    }
}
