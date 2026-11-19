// Adapted from Google Material Symbols (Rounded, inbox, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Inbox: ImageVector
    get() = inbox

private val inbox by lazy {
    materialSymbol("Rounded.Inbox") {
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
        moveToRelative(7f, -5f)
        quadToRelative(0.8f, 0f, 1.48f, -0.41f)
        reflectiveQuadToRelative(1.1f, -1.09f)
        quadToRelative(0.15f, -0.23f, 0.38f, -0.36f)
        reflectiveQuadTo(15.45f, 14f)
        horizontalLineTo(19f)
        verticalLineTo(5f)
        horizontalLineTo(5f)
        verticalLineToRelative(9f)
        horizontalLineTo(8.55f)
        quadToRelative(0.28f, 0f, 0.5f, 0.14f)
        reflectiveQuadTo(9.43f, 14.5f)
        quadToRelative(0.43f, 0.67f, 1.1f, 1.09f)
        reflectiveQuadTo(12f, 16f)
        close()
    }
}
