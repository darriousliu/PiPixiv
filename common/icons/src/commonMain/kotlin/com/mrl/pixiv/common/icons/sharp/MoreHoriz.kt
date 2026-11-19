// Adapted from Google Material Symbols (Sharp, more_horiz, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.MoreHoriz: ImageVector
    get() = moreHoriz

private val moreHoriz by lazy {
    materialSymbol("Sharp.MoreHoriz") {
        moveTo(6f, 14f)
        quadTo(5.18f, 14f, 4.59f, 13.41f)
        reflectiveQuadTo(4f, 12f)
        reflectiveQuadTo(4.59f, 10.59f)
        reflectiveQuadTo(6f, 10f)
        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
        quadTo(8f, 11.18f, 8f, 12f)
        reflectiveQuadTo(7.41f, 13.41f)
        reflectiveQuadTo(6f, 14f)
        close()
        moveToRelative(6f, 0f)
        quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
        reflectiveQuadTo(10f, 12f)
        reflectiveQuadToRelative(0.59f, -1.41f)
        reflectiveQuadTo(12f, 10f)
        reflectiveQuadToRelative(1.41f, 0.59f)
        quadTo(14f, 11.18f, 14f, 12f)
        reflectiveQuadToRelative(-0.59f, 1.41f)
        reflectiveQuadTo(12f, 14f)
        close()
        moveToRelative(6f, 0f)
        quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
        reflectiveQuadTo(16f, 12f)
        reflectiveQuadToRelative(0.59f, -1.41f)
        reflectiveQuadTo(18f, 10f)
        reflectiveQuadToRelative(1.41f, 0.59f)
        quadTo(20f, 11.18f, 20f, 12f)
        reflectiveQuadToRelative(-0.59f, 1.41f)
        reflectiveQuadTo(18f, 14f)
        close()
    }
}
