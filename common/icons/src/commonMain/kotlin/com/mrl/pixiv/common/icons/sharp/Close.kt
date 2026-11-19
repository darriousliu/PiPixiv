// Adapted from Google Material Symbols (Sharp, close, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Close: ImageVector
    get() = close

private val close by lazy {
    materialSymbol("Sharp.Close") {
        moveTo(6.4f, 19f)
        lineTo(5f, 17.6f)
        lineTo(10.6f, 12f)
        lineTo(5f, 6.4f)
        lineTo(6.4f, 5f)
        lineTo(12f, 10.6f)
        lineTo(17.6f, 5f)
        lineTo(19f, 6.4f)
        lineTo(13.4f, 12f)
        lineTo(19f, 17.6f)
        lineTo(17.6f, 19f)
        lineTo(12f, 13.4f)
        lineTo(6.4f, 19f)
        close()
    }
}
