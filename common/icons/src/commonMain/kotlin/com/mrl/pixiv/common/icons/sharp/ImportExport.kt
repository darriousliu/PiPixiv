// Adapted from Google Material Symbols (Sharp, import_export, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.ImportExport: ImageVector
    get() = importExport

private val importExport by lazy {
    materialSymbol("Sharp.ImportExport") {
        moveTo(8f, 13f)
        verticalLineTo(5.82f)
        lineTo(5.43f, 8.4f)
        lineTo(4f, 7f)
        lineTo(9f, 2f)
        lineToRelative(5f, 5f)
        lineTo(12.58f, 8.4f)
        lineTo(10f, 5.82f)
        verticalLineTo(13f)
        horizontalLineTo(8f)
        close()
        moveToRelative(7f, 9f)
        lineTo(10f, 17f)
        lineToRelative(1.43f, -1.4f)
        lineTo(14f, 18.18f)
        verticalLineTo(11f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(7.18f)
        lineTo(18.58f, 15.6f)
        lineTo(20f, 17f)
        lineToRelative(-5f, 5f)
        close()
    }
}
