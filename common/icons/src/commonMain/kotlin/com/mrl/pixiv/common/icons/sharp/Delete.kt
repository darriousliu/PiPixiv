// Adapted from Google Material Symbols (Sharp, delete, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Delete: ImageVector
    get() = delete

private val delete by lazy {
    materialSymbol("Sharp.Delete") {
        moveTo(9f, 17f)
        horizontalLineToRelative(2f)
        verticalLineTo(8f)
        horizontalLineTo(9f)
        verticalLineToRelative(9f)
        close()
        moveToRelative(4f, 0f)
        horizontalLineToRelative(2f)
        verticalLineTo(8f)
        horizontalLineTo(13f)
        verticalLineToRelative(9f)
        close()
        moveTo(5f, 21f)
        verticalLineTo(6f)
        horizontalLineTo(4f)
        verticalLineTo(4f)
        horizontalLineTo(9f)
        verticalLineTo(3f)
        horizontalLineToRelative(6f)
        verticalLineTo(4f)
        horizontalLineToRelative(5f)
        verticalLineTo(6f)
        horizontalLineTo(19f)
        verticalLineTo(21f)
        horizontalLineTo(5f)
        close()
    }
}
