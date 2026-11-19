// Adapted from Google Material Symbols (Sharp, add, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Add: ImageVector
    get() = add

private val add by lazy {
    materialSymbol("Sharp.Add") {
        moveTo(11f, 13f)
        horizontalLineTo(5f)
        verticalLineTo(11f)
        horizontalLineToRelative(6f)
        verticalLineTo(5f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(6f)
        horizontalLineToRelative(6f)
        verticalLineToRelative(2f)
        horizontalLineTo(13f)
        verticalLineToRelative(6f)
        horizontalLineTo(11f)
        verticalLineTo(13f)
        close()
    }
}
