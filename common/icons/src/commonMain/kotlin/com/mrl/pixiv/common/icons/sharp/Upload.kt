// Adapted from Google Material Symbols (Sharp, upload, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Upload: ImageVector
    get() = upload

private val upload by lazy {
    materialSymbol("Sharp.Upload") {
        moveTo(11f, 16f)
        verticalLineTo(7.85f)
        lineToRelative(-2.6f, 2.6f)
        lineTo(7f, 9f)
        lineTo(12f, 4f)
        lineToRelative(5f, 5f)
        lineToRelative(-1.4f, 1.45f)
        lineTo(13f, 7.85f)
        verticalLineTo(16f)
        horizontalLineTo(11f)
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
