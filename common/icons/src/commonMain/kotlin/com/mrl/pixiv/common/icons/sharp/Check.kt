// Adapted from Google Material Symbols (Sharp, check, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Check: ImageVector
    get() = check

private val check by lazy {
    materialSymbol("Sharp.Check") {
        moveTo(9.55f, 18f)
        lineTo(3.85f, 12.3f)
        lineTo(5.28f, 10.88f)
        lineToRelative(4.28f, 4.28f)
        lineTo(18.73f, 5.97f)
        lineTo(20.15f, 7.4f)
        lineTo(9.55f, 18f)
        close()
    }
}
