// Adapted from Google Material Symbols (Outlined, network_wifi, FILL=1).
// Source: https://fonts.google.com/icons
// SPDX-License-Identifier: Apache-2.0

package com.mrl.pixiv.common.icons.outlined

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Outlined.NetworkWifi: ImageVector
    get() = networkWifi

private val networkWifi by lazy {
    materialSymbol("Outlined.NetworkWifi") {
        moveTo(12f, 21f)
        lineTo(0f, 9f)
        quadTo(2.38f, 6.57f, 5.49f, 5.29f)
        reflectiveQuadTo(12f, 4f)
        quadToRelative(3.43f, 0f, 6.53f, 1.27f)
        reflectiveQuadTo(24f, 9f)
        lineTo(12f, 21f)
        close()
        moveTo(4.35f, 10.5f)
        quadTo(6.03f, 9.3f, 7.98f, 8.65f)
        reflectiveQuadTo(12f, 8f)
        reflectiveQuadToRelative(4.03f, 0.65f)
        reflectiveQuadToRelative(3.63f, 1.85f)
        lineTo(21.1f, 9.05f)
        quadTo(19.13f, 7.55f, 16.8f, 6.77f)
        reflectiveQuadTo(12f, 6f)
        quadTo(9.53f, 6f, 7.2f, 6.77f)
        reflectiveQuadTo(2.9f, 9.05f)
        lineTo(4.35f, 10.5f)
        close()
    }
}
