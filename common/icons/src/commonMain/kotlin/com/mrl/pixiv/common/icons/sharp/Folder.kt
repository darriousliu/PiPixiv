// Adapted from Google Material Symbols (Sharp, folder, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Folder: ImageVector
    get() = folder

private val folder by lazy {
    materialSymbol("Sharp.Folder") {
        moveTo(2f, 20f)
        verticalLineTo(4f)
        horizontalLineToRelative(8f)
        lineToRelative(2f, 2f)
        horizontalLineTo(22f)
        verticalLineTo(20f)
        horizontalLineTo(2f)
        close()
    }
}
