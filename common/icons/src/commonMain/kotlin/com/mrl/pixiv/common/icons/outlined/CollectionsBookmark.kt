// Adapted from Google Material Symbols (Outlined, collections_bookmark, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.CollectionsBookmark: ImageVector
    get() = collectionsBookmark

private val collectionsBookmark by lazy {
    materialSymbol("Outlined.CollectionsBookmark") {
        moveTo(4f, 22f)
        quadTo(3.18f, 22f, 2.59f, 21.41f)
        reflectiveQuadTo(2f, 20f)
        verticalLineTo(6f)
        horizontalLineTo(4f)
        verticalLineTo(20f)
        horizontalLineTo(18f)
        verticalLineToRelative(2f)
        horizontalLineTo(4f)
        close()
        moveTo(8f, 18f)
        quadTo(7.18f, 18f, 6.59f, 17.41f)
        reflectiveQuadTo(6f, 16f)
        verticalLineTo(4f)
        quadTo(6f, 3.17f, 6.59f, 2.59f)
        reflectiveQuadTo(8f, 2f)
        horizontalLineTo(20f)
        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
        reflectiveQuadTo(22f, 4f)
        verticalLineTo(16f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(20f, 18f)
        horizontalLineTo(8f)
        close()
        moveToRelative(5f, -7f)
        lineTo(15.5f, 9.5f)
        lineTo(18f, 11f)
        verticalLineTo(4f)
        horizontalLineTo(13f)
        verticalLineToRelative(7f)
        close()
    }
}
