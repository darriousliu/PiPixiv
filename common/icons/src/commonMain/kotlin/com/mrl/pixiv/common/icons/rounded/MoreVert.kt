// Adapted from Google Material Symbols (Rounded, more_vert, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.MoreVert: ImageVector
    get() = moreVert

private val moreVert by lazy {
    materialSymbol("Rounded.MoreVert") {
        moveTo(12f, 20f)
        quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
        reflectiveQuadTo(10f, 18f)
        reflectiveQuadToRelative(0.59f, -1.41f)
        reflectiveQuadTo(12f, 16f)
        reflectiveQuadToRelative(1.41f, 0.59f)
        quadTo(14f, 17.18f, 14f, 18f)
        reflectiveQuadToRelative(-0.59f, 1.41f)
        reflectiveQuadTo(12f, 20f)
        close()
        moveToRelative(0f, -6f)
        quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
        reflectiveQuadTo(10f, 12f)
        reflectiveQuadToRelative(0.59f, -1.41f)
        reflectiveQuadTo(12f, 10f)
        reflectiveQuadToRelative(1.41f, 0.59f)
        quadTo(14f, 11.18f, 14f, 12f)
        reflectiveQuadToRelative(-0.59f, 1.41f)
        reflectiveQuadTo(12f, 14f)
        close()
        moveTo(12f, 8f)
        quadTo(11.18f, 8f, 10.59f, 7.41f)
        reflectiveQuadTo(10f, 6f)
        reflectiveQuadTo(10.59f, 4.59f)
        reflectiveQuadTo(12f, 4f)
        reflectiveQuadToRelative(1.41f, 0.59f)
        quadTo(14f, 5.18f, 14f, 6f)
        reflectiveQuadTo(13.41f, 7.41f)
        reflectiveQuadTo(12f, 8f)
        close()
    }
}
