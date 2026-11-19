// Adapted from Google Material Symbols (Sharp, content_copy, FILL=0).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.ContentCopy: ImageVector
    get() = contentCopy

private val contentCopy by lazy {
    materialSymbol("Sharp.ContentCopy") {
        moveTo(7f, 18f)
        verticalLineTo(2f)
        horizontalLineTo(20f)
        verticalLineTo(18f)
        horizontalLineTo(7f)
        close()
        moveTo(9f, 16f)
        horizontalLineToRelative(9f)
        verticalLineTo(4f)
        horizontalLineTo(9f)
        verticalLineTo(16f)
        close()
        moveTo(3f, 22f)
        verticalLineTo(6f)
        horizontalLineTo(5f)
        verticalLineTo(20f)
        horizontalLineTo(16f)
        verticalLineToRelative(2f)
        horizontalLineTo(3f)
        close()
        moveTo(9f, 16f)
        verticalLineTo(4f)
        verticalLineTo(16f)
        close()
    }
}
