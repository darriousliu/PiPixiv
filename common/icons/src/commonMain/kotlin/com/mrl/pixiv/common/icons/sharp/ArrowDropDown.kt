// Adapted from Google Material Symbols (Sharp, arrow_drop_down, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.ArrowDropDown: ImageVector
    get() = arrowDropDown

private val arrowDropDown by lazy {
    materialSymbol("Sharp.ArrowDropDown") {
        moveTo(12f, 15f)
        lineTo(7f, 10f)
        horizontalLineTo(17f)
        lineToRelative(-5f, 5f)
        close()
    }
}
