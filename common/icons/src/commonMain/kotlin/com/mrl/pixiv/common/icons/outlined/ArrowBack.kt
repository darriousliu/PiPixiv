// Adapted from Google Material Symbols (Outlined, arrow_back, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.ArrowBack: ImageVector
    get() = arrowBack

private val arrowBack by lazy {
    materialSymbol("Outlined.ArrowBack", autoMirror = true) {
        moveTo(7.83f, 13f)
        lineToRelative(5.6f, 5.6f)
        lineTo(12f, 20f)
        lineTo(4f, 12f)
        lineTo(12f, 4f)
        lineToRelative(1.43f, 1.4f)
        lineTo(7.83f, 11f)
        horizontalLineTo(20f)
        verticalLineToRelative(2f)
        horizontalLineTo(7.83f)
        close()
    }
}
