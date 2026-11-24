// 改编自 Google Material Symbols 圆角填充图标（Apache-2.0）。
// 来源：https://github.com/google/material-design-icons

package com.mrl.pixiv.common.icons.rounded

import androidx.compose.ui.graphics.vector.ImageVector
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.materialSymbol

val Icons.Rounded.Colorize: ImageVector
    get() = colorize

private val colorize by lazy {
    materialSymbol("Rounded.Colorize") {
        moveTo(3f, 20f)
        verticalLineToRelative(-2.925f)
        quadToRelative(0f, -0.4f, 0.15f, -0.7625f)
        reflectiveQuadToRelative(0.425f, -0.6375f)
        lineToRelative(8.375f, -8.375f)
        lineToRelative(-1.45f, -1.4f)
        lineToRelative(1.45f, -1.4f)
        lineToRelative(1.9f, 1.9f)
        lineToRelative(3.1f, -3.1f)
        quadToRelative(0.125f, -0.125f, 0.3125f, -0.2f)
        reflectiveQuadToRelative(0.3875f, -0.075f)
        quadToRelative(0.2f, 0f, 0.375f, 0.075f)
        reflectiveQuadToRelative(0.325f, 0.2f)
        lineToRelative(2.35f, 2.35f)
        quadToRelative(0.125f, 0.15f, 0.2f, 0.325f)
        reflectiveQuadToRelative(0.075f, 0.375f)
        quadToRelative(0f, 0.2f, -0.075f, 0.3875f)
        reflectiveQuadToRelative(-0.2f, 0.3125f)
        lineTo(17.625f, 10.125f)
        lineToRelative(1.9f, 1.95f)
        lineToRelative(-1.425f, 1.425f)
        lineToRelative(-1.4f, -1.45f)
        lineToRelative(-8.375f, 8.375f)
        quadToRelative(-0.275f, 0.275f, -0.6375f, 0.425f)
        reflectiveQuadToRelative(-0.7625f, 0.15f)
        horizontalLineTo(4f)
        quadToRelative(-0.425f, 0f, -0.7125f, -0.2875f)
        reflectiveQuadTo(3f, 20f)
        close()
        moveToRelative(2f, -1f)
        horizontalLineToRelative(1.95f)
        lineToRelative(8.3f, -8.35f)
        lineToRelative(-1.9f, -1.9f)
        lineToRelative(-8.35f, 8.3f)
        verticalLineToRelative(1.95f)
        close()
    }
}
