// Adapted from Google Material Symbols (Sharp, equalizer, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Equalizer: ImageVector
    get() = equalizer

private val equalizer by lazy {
    materialSymbol("Sharp.Equalizer") {
        moveTo(4f, 20f)
        verticalLineTo(12f)
        horizontalLineTo(8f)
        verticalLineToRelative(8f)
        horizontalLineTo(4f)
        close()
        moveToRelative(6f, 0f)
        verticalLineTo(4f)
        horizontalLineToRelative(4f)
        verticalLineTo(20f)
        horizontalLineTo(10f)
        close()
        moveToRelative(6f, 0f)
        verticalLineTo(9f)
        horizontalLineToRelative(4f)
        verticalLineTo(20f)
        horizontalLineTo(16f)
        close()
    }
}
