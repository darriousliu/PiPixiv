// Adapted from Google Material Symbols (Sharp, expand_more, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.ExpandMore: ImageVector
    get() = expandMore

private val expandMore by lazy {
    materialSymbol("Sharp.ExpandMore") {
        moveTo(12f, 15.38f)
        lineToRelative(-6f, -6f)
        lineTo(7.4f, 7.97f)
        lineToRelative(4.6f, 4.6f)
        lineToRelative(4.6f, -4.6f)
        lineTo(18f, 9.38f)
        lineToRelative(-6f, 6f)
        close()
    }
}
