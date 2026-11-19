// Adapted from Google Material Symbols (Outlined, person_off, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.PersonOff: ImageVector
    get() = personOff

private val personOff by lazy {
    materialSymbol("Outlined.PersonOff") {
        moveTo(19.78f, 22.63f)
        lineTo(17.15f, 20f)
        horizontalLineTo(4f)
        verticalLineTo(17.2f)
        quadTo(4f, 16.35f, 4.44f, 15.64f)
        quadTo(4.88f, 14.93f, 5.6f, 14.55f)
        quadTo(6.73f, 13.98f, 7.89f, 13.63f)
        quadTo(9.05f, 13.27f, 10.25f, 13.1f)
        lineTo(1.38f, 4.22f)
        lineTo(2.8f, 2.8f)
        lineTo(21.2f, 21.2f)
        lineToRelative(-1.43f, 1.43f)
        close()
        moveTo(18.4f, 14.55f)
        quadToRelative(0.72f, 0.35f, 1.15f, 1.06f)
        quadToRelative(0.43f, 0.71f, 0.45f, 1.54f)
        lineTo(16.65f, 13.8f)
        quadToRelative(0.45f, 0.18f, 0.89f, 0.35f)
        quadToRelative(0.44f, 0.18f, 0.86f, 0.4f)
        close()
        moveToRelative(-4.2f, -3.2f)
        lineTo(8.65f, 5.8f)
        quadTo(9.23f, 4.95f, 10.1f, 4.47f)
        reflectiveQuadTo(12f, 4f)
        quadToRelative(1.65f, 0f, 2.83f, 1.18f)
        reflectiveQuadTo(16f, 8f)
        quadToRelative(0f, 1.02f, -0.47f, 1.9f)
        reflectiveQuadTo(14.2f, 11.35f)
        close()
    }
}
