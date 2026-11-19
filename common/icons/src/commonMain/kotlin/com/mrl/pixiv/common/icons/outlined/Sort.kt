// Adapted from Google Material Symbols (Outlined, sort, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Sort: ImageVector
    get() = sort

private val sort by lazy {
    materialSymbol("Outlined.Sort", autoMirror = true) {
        moveTo(3f, 18f)
        verticalLineTo(16f)
        horizontalLineTo(9f)
        verticalLineToRelative(2f)
        horizontalLineTo(3f)
        close()
        moveTo(3f, 13f)
        verticalLineTo(11f)
        horizontalLineTo(15f)
        verticalLineToRelative(2f)
        horizontalLineTo(3f)
        close()
        moveTo(3f, 8f)
        verticalLineTo(6f)
        horizontalLineTo(21f)
        verticalLineTo(8f)
        horizontalLineTo(3f)
        close()
    }
}
