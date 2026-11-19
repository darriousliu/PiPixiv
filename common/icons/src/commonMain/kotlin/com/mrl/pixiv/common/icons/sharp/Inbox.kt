// Adapted from Google Material Symbols (Sharp, inbox, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Inbox: ImageVector
    get() = inbox

private val inbox by lazy {
    materialSymbol("Sharp.Inbox") {
        moveTo(3f, 21f)
        verticalLineTo(3f)
        horizontalLineTo(21f)
        verticalLineTo(21f)
        horizontalLineTo(3f)
        close()
        moveTo(13.73f, 15.45f)
        quadTo(14.5f, 14.9f, 14.8f, 14f)
        horizontalLineTo(19f)
        verticalLineTo(5f)
        horizontalLineTo(5f)
        verticalLineToRelative(9f)
        horizontalLineTo(9.2f)
        quadToRelative(0.3f, 0.9f, 1.07f, 1.45f)
        reflectiveQuadTo(12f, 16f)
        reflectiveQuadToRelative(1.73f, -0.55f)
        close()
    }
}
