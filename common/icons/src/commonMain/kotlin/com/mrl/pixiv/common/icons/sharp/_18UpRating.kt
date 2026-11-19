// Adapted from Google Material Symbols (Sharp, 18_up_rating, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp._18UpRating: ImageVector
    get() = rating18Up

private val rating18Up by lazy {
    materialSymbol("Sharp._18UpRating") {
        moveTo(8.5f, 15f)
        horizontalLineTo(10f)
        verticalLineTo(9f)
        horizontalLineTo(7f)
        verticalLineToRelative(1.5f)
        horizontalLineTo(8.5f)
        verticalLineTo(15f)
        close()
        moveToRelative(3f, 0f)
        horizontalLineTo(16f)
        verticalLineTo(9f)
        horizontalLineTo(11.5f)
        verticalLineToRelative(6f)
        close()
        moveTo(13f, 14f)
        verticalLineTo(12.5f)
        horizontalLineToRelative(1.5f)
        verticalLineTo(14f)
        horizontalLineTo(13f)
        close()
        moveToRelative(0f, -2.5f)
        verticalLineTo(10f)
        horizontalLineToRelative(1.5f)
        verticalLineToRelative(1.5f)
        horizontalLineTo(13f)
        close()
        moveTo(3f, 21f)
        verticalLineTo(3f)
        horizontalLineTo(21f)
        verticalLineTo(21f)
        horizontalLineTo(3f)
        close()
    }
}
