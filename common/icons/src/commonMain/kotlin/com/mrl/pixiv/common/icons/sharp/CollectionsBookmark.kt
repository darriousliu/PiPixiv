// Adapted from Google Material Symbols (Sharp, collections_bookmark, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.sharp

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Sharp.CollectionsBookmark: ImageVector
    get() = collectionsBookmark

private val collectionsBookmark by lazy {
    materialSymbol("Sharp.CollectionsBookmark") {
        moveTo(6f, 18f)
        verticalLineTo(2f)
        horizontalLineTo(22f)
        verticalLineTo(18f)
        horizontalLineTo(6f)
        close()
        moveTo(2f, 22f)
        verticalLineTo(6f)
        horizontalLineTo(4f)
        verticalLineTo(20f)
        horizontalLineTo(18f)
        verticalLineToRelative(2f)
        horizontalLineTo(2f)
        close()
        moveTo(13f, 4f)
        verticalLineToRelative(7f)
        lineTo(15.5f, 9.5f)
        lineTo(18f, 11f)
        verticalLineTo(4f)
        horizontalLineTo(13f)
        close()
    }
}
