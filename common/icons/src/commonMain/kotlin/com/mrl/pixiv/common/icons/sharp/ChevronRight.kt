// Adapted from Google Material Symbols (Sharp, chevron_right, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.ChevronRight: ImageVector
    get() = chevronRight

private val chevronRight by lazy {
    materialSymbol("Sharp.ChevronRight") {
        moveTo(12.6f, 12f)
        lineTo(8f, 7.4f)
        lineTo(9.4f, 6f)
        lineToRelative(6f, 6f)
        lineToRelative(-6f, 6f)
        lineTo(8f, 16.6f)
        lineTo(12.6f, 12f)
        close()
    }
}
