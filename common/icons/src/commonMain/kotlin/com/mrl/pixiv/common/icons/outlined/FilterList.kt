// Adapted from Google Material Symbols (Outlined, filter_list, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.FilterList: ImageVector
    get() = filterList

private val filterList by lazy {
    materialSymbol("Outlined.FilterList") {
        moveTo(10f, 18f)
        verticalLineTo(16f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(2f)
        horizontalLineTo(10f)
        close()
        moveTo(6f, 13f)
        verticalLineTo(11f)
        horizontalLineTo(18f)
        verticalLineToRelative(2f)
        horizontalLineTo(6f)
        close()
        moveTo(3f, 8f)
        verticalLineTo(6f)
        horizontalLineTo(21f)
        verticalLineTo(8f)
        horizontalLineTo(3f)
        close()
    }
}
