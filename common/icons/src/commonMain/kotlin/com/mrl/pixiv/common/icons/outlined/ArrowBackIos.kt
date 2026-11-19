// Adapted from Google Material Symbols (Outlined, arrow_back_ios_new, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.ArrowBackIos: ImageVector
    get() = arrowBackIos

private val arrowBackIos by lazy {
    materialSymbol("Outlined.ArrowBackIos", autoMirror = true) {
        moveTo(16f, 22f)
        lineTo(6f, 12f)
        lineTo(16f, 2f)
        lineToRelative(1.78f, 1.77f)
        lineTo(9.55f, 12f)
        lineToRelative(8.23f, 8.23f)
        lineTo(16f, 22f)
        close()
    }
}
