// Adapted from Google Material Symbols (Sharp, filter_alt, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.FilterAlt: ImageVector
    get() = filterAlt

private val filterAlt by lazy {
    materialSymbol("Sharp.FilterAlt") {
        moveTo(10f, 20f)
        verticalLineTo(13f)
        lineTo(2.95f, 4f)
        horizontalLineToRelative(18.1f)
        lineTo(14f, 13f)
        verticalLineToRelative(7f)
        horizontalLineTo(10f)
        close()
    }
}
