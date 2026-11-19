// Adapted from Google Material Symbols (Sharp, file_download, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.FileDownload: ImageVector
    get() = fileDownload

private val fileDownload by lazy {
    materialSymbol("Sharp.FileDownload") {
        moveTo(12f, 16f)
        lineTo(7f, 11f)
        lineTo(8.4f, 9.55f)
        lineToRelative(2.6f, 2.6f)
        verticalLineTo(4f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(8.15f)
        lineToRelative(2.6f, -2.6f)
        lineTo(17f, 11f)
        lineToRelative(-5f, 5f)
        close()
        moveTo(4f, 20f)
        verticalLineTo(15f)
        horizontalLineTo(6f)
        verticalLineToRelative(3f)
        horizontalLineTo(18f)
        verticalLineTo(15f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(5f)
        horizontalLineTo(4f)
        close()
    }
}
