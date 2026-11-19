// Adapted from Google Material Symbols (Sharp, file_copy, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.FileCopy: ImageVector
    get() = fileCopy

private val fileCopy by lazy {
    materialSymbol("Sharp.FileCopy") {
        moveTo(6f, 19f)
        verticalLineTo(1f)
        horizontalLineToRelative(9f)
        lineToRelative(6f, 6f)
        verticalLineTo(19f)
        horizontalLineTo(6f)
        close()
        moveTo(14f, 8f)
        horizontalLineToRelative(5f)
        lineTo(14f, 3f)
        verticalLineTo(8f)
        close()
        moveTo(2f, 23f)
        verticalLineTo(7f)
        horizontalLineTo(4f)
        verticalLineTo(21f)
        horizontalLineTo(15f)
        verticalLineToRelative(2f)
        horizontalLineTo(2f)
        close()
    }
}
