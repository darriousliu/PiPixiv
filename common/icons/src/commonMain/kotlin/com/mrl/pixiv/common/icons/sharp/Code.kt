// Adapted from Google Material Symbols (Sharp, code, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Code: ImageVector
    get() = code

private val code by lazy {
    materialSymbol("Sharp.Code") {
        moveTo(8f, 18f)
        lineTo(2f, 12f)
        lineTo(8f, 6f)
        lineTo(9.43f, 7.43f)
        lineToRelative(-4.6f, 4.6f)
        lineTo(9.4f, 16.6f)
        lineTo(8f, 18f)
        close()
        moveToRelative(8f, 0f)
        lineTo(14.58f, 16.58f)
        lineToRelative(4.6f, -4.6f)
        lineTo(14.6f, 7.4f)
        lineTo(16f, 6f)
        lineToRelative(6f, 6f)
        lineToRelative(-6f, 6f)
        close()
    }
}
