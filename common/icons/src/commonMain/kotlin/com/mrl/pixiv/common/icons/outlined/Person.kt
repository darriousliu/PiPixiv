// Adapted from Google Material Symbols (Outlined, person, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.Person: ImageVector
    get() = person

private val person by lazy {
    materialSymbol("Outlined.Person") {
        moveTo(9.18f, 10.83f)
        quadTo(8f, 9.65f, 8f, 8f)
        reflectiveQuadTo(9.18f, 5.18f)
        reflectiveQuadTo(12f, 4f)
        reflectiveQuadToRelative(2.83f, 1.18f)
        reflectiveQuadTo(16f, 8f)
        reflectiveQuadToRelative(-1.17f, 2.82f)
        reflectiveQuadTo(12f, 12f)
        reflectiveQuadTo(9.18f, 10.83f)
        close()
        moveTo(4f, 20f)
        verticalLineTo(17.2f)
        quadTo(4f, 16.35f, 4.44f, 15.64f)
        quadTo(4.88f, 14.93f, 5.6f, 14.55f)
        quadTo(7.15f, 13.77f, 8.75f, 13.39f)
        reflectiveQuadTo(12f, 13f)
        reflectiveQuadToRelative(3.25f, 0.39f)
        reflectiveQuadToRelative(3.15f, 1.16f)
        quadToRelative(0.72f, 0.38f, 1.16f, 1.09f)
        reflectiveQuadTo(20f, 17.2f)
        verticalLineTo(20f)
        horizontalLineTo(4f)
        close()
    }
}
