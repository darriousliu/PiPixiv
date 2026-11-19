// Adapted from Google Material Symbols (Sharp, expand_less, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.ExpandLess: ImageVector
    get() = expandLess

private val expandLess by lazy {
    materialSymbol("Sharp.ExpandLess") {
        moveTo(7.4f, 15.38f)
        lineTo(6f, 13.98f)
        lineToRelative(6f, -6f)
        lineToRelative(6f, 6f)
        lineToRelative(-1.4f, 1.4f)
        lineTo(12f, 10.77f)
        lineToRelative(-4.6f, 4.6f)
        close()
    }
}
