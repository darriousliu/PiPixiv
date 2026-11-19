// Adapted from Google Material Symbols (Sharp, text_fields, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.TextFields: ImageVector
    get() = textFields

private val textFields by lazy {
    materialSymbol("Sharp.TextFields") {
        moveTo(7f, 20f)
        verticalLineTo(7f)
        horizontalLineTo(2f)
        verticalLineTo(4f)
        horizontalLineTo(15f)
        verticalLineTo(7f)
        horizontalLineTo(10f)
        verticalLineTo(20f)
        horizontalLineTo(7f)
        close()
        moveToRelative(9f, 0f)
        verticalLineTo(12f)
        horizontalLineTo(13f)
        verticalLineTo(9f)
        horizontalLineToRelative(9f)
        verticalLineToRelative(3f)
        horizontalLineTo(19f)
        verticalLineToRelative(8f)
        horizontalLineTo(16f)
        close()
    }
}
