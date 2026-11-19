// Adapted from Google Material Symbols (Sharp, person_add, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.PersonAdd: ImageVector
    get() = personAdd

private val personAdd by lazy {
    materialSymbol("Sharp.PersonAdd") {
        moveTo(18f, 14f)
        verticalLineTo(11f)
        horizontalLineTo(15f)
        verticalLineTo(9f)
        horizontalLineToRelative(3f)
        verticalLineTo(6f)
        horizontalLineToRelative(2f)
        verticalLineTo(9f)
        horizontalLineToRelative(3f)
        verticalLineToRelative(2f)
        horizontalLineTo(20f)
        verticalLineToRelative(3f)
        horizontalLineTo(18f)
        close()
        moveTo(6.18f, 10.83f)
        quadTo(5f, 9.65f, 5f, 8f)
        reflectiveQuadTo(6.18f, 5.18f)
        reflectiveQuadTo(9f, 4f)
        reflectiveQuadToRelative(2.83f, 1.18f)
        reflectiveQuadTo(13f, 8f)
        reflectiveQuadToRelative(-1.17f, 2.82f)
        reflectiveQuadTo(9f, 12f)
        reflectiveQuadTo(6.18f, 10.83f)
        close()
        moveTo(1f, 20f)
        verticalLineTo(17.2f)
        quadTo(1f, 16.35f, 1.44f, 15.64f)
        quadTo(1.88f, 14.93f, 2.6f, 14.55f)
        quadTo(4.15f, 13.77f, 5.75f, 13.39f)
        reflectiveQuadTo(9f, 13f)
        reflectiveQuadToRelative(3.25f, 0.39f)
        reflectiveQuadToRelative(3.15f, 1.16f)
        quadToRelative(0.72f, 0.38f, 1.16f, 1.09f)
        reflectiveQuadTo(17f, 17.2f)
        verticalLineTo(20f)
        horizontalLineTo(1f)
        close()
    }
}
