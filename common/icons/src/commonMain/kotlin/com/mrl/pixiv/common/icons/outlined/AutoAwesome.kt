// Adapted from Google Material Symbols (Outlined, auto_awesome, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.AutoAwesome: ImageVector
    get() = autoAwesome

private val autoAwesome by lazy {
    materialSymbol("Outlined.AutoAwesome") {
        moveTo(19f, 9f)
        lineTo(17.75f, 6.25f)
        lineTo(15f, 5f)
        lineTo(17.75f, 3.75f)
        lineTo(19f, 1f)
        lineToRelative(1.25f, 2.75f)
        lineTo(23f, 5f)
        lineTo(20.25f, 6.25f)
        lineTo(19f, 9f)
        close()
        moveToRelative(0f, 14f)
        lineTo(17.75f, 20.25f)
        lineTo(15f, 19f)
        lineToRelative(2.75f, -1.25f)
        lineTo(19f, 15f)
        lineToRelative(1.25f, 2.75f)
        lineTo(23f, 19f)
        lineToRelative(-2.75f, 1.25f)
        lineTo(19f, 23f)
        close()
        moveTo(9f, 20f)
        lineTo(6.5f, 14.5f)
        lineTo(1f, 12f)
        lineTo(6.5f, 9.5f)
        lineTo(9f, 4f)
        lineToRelative(2.5f, 5.5f)
        lineTo(17f, 12f)
        lineToRelative(-5.5f, 2.5f)
        lineTo(9f, 20f)
        close()
    }
}
