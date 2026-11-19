// Adapted from Google Material Symbols (Outlined, inbox, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Inbox: ImageVector
    get() = inbox

private val inbox by lazy {
    materialSymbol("Outlined.Inbox") {
        moveTo(5f, 21f)
        quadTo(4.18f, 21f, 3.59f, 20.41f)
        reflectiveQuadTo(3f, 19f)
        verticalLineTo(5f)
        quadTo(3f, 4.17f, 3.59f, 3.59f)
        reflectiveQuadTo(5f, 3f)
        horizontalLineTo(19f)
        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
        reflectiveQuadTo(21f, 5f)
        verticalLineTo(19f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(19f, 21f)
        horizontalLineTo(5f)
        close()
        moveToRelative(8.73f, -5.55f)
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
