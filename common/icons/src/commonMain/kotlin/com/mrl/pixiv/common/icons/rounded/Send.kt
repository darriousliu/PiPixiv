// Adapted from Google Material Symbols (Rounded, send, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Send: ImageVector
    get() = send

private val send by lazy {
    materialSymbol("Rounded.Send", autoMirror = true) {
        moveTo(4.4f, 19.43f)
        quadTo(3.9f, 19.63f, 3.45f, 19.34f)
        reflectiveQuadTo(3f, 18.5f)
        verticalLineTo(14f)
        lineToRelative(8f, -2f)
        lineTo(3f, 10f)
        verticalLineTo(5.5f)
        quadTo(3f, 4.95f, 3.45f, 4.66f)
        quadTo(3.9f, 4.38f, 4.4f, 4.57f)
        lineToRelative(15.4f, 6.5f)
        quadToRelative(0.63f, 0.28f, 0.63f, 0.93f)
        reflectiveQuadTo(19.8f, 12.93f)
        lineTo(4.4f, 19.43f)
        close()
    }
}
