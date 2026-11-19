// Adapted from Google Material Symbols (Sharp, bookmark, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Bookmark: ImageVector
    get() = bookmark

private val bookmark by lazy {
    materialSymbol("Sharp.Bookmark") {
        moveTo(5f, 21f)
        verticalLineTo(3f)
        horizontalLineTo(19f)
        verticalLineTo(21f)
        lineTo(12f, 18f)
        lineTo(5f, 21f)
        close()
    }
}
