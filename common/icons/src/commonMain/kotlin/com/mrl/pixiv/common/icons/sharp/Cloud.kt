// Adapted from Google Material Symbols (Sharp, cloud, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.Cloud: ImageVector
    get() = cloud

private val cloud by lazy {
    materialSymbol("Sharp.Cloud") {
        moveTo(6.5f, 20f)
        quadTo(4.23f, 20f, 2.61f, 18.43f)
        reflectiveQuadTo(1f, 14.58f)
        quadTo(1f, 12.63f, 2.18f, 11.1f)
        reflectiveQuadTo(5.25f, 9.15f)
        quadTo(5.88f, 6.85f, 7.75f, 5.43f)
        reflectiveQuadTo(12f, 4f)
        quadToRelative(2.93f, 0f, 4.96f, 2.04f)
        reflectiveQuadTo(19f, 11f)
        quadToRelative(1.73f, 0.2f, 2.86f, 1.49f)
        reflectiveQuadTo(23f, 15.5f)
        quadToRelative(0f, 1.88f, -1.31f, 3.19f)
        reflectiveQuadTo(18.5f, 20f)
        horizontalLineTo(6.5f)
        close()
    }
}
