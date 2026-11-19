// Adapted from Google Material Symbols (Rounded, collections_bookmark, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.CollectionsBookmark: ImageVector
    get() = collectionsBookmark

private val collectionsBookmark by lazy {
    materialSymbol("Rounded.CollectionsBookmark") {
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
        moveTo(4f, 22f)
        quadTo(3.18f, 22f, 2.59f, 21.41f)
        reflectiveQuadTo(2f, 20f)
        verticalLineTo(7f)
        quadTo(2f, 6.57f, 2.29f, 6.29f)
        reflectiveQuadTo(3f, 6f)
        reflectiveQuadTo(3.71f, 6.29f)
        reflectiveQuadTo(4f, 7f)
        verticalLineTo(20f)
        horizontalLineTo(17f)
        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
        reflectiveQuadTo(18f, 21f)
        reflectiveQuadToRelative(-0.29f, 0.71f)
        reflectiveQuadTo(17f, 22f)
        horizontalLineTo(4f)
        close()
        moveTo(13f, 10.13f)
        quadToRelative(0f, 0.3f, 0.25f, 0.44f)
        reflectiveQuadToRelative(0.5f, -0.01f)
        lineTo(14.98f, 9.8f)
        quadTo(15.23f, 9.65f, 15.5f, 9.65f)
        quadToRelative(0.28f, 0f, 0.53f, 0.15f)
        lineToRelative(1.22f, 0.75f)
        quadToRelative(0.25f, 0.15f, 0.5f, 0.01f)
        reflectiveQuadTo(18f, 10.13f)
        verticalLineTo(4f)
        horizontalLineTo(13f)
        verticalLineToRelative(6.13f)
        close()
    }
}
