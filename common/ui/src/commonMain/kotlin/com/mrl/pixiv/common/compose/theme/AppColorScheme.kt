package com.mrl.pixiv.common.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme
import com.mrl.pixiv.common.data.setting.AppPaletteStyle
import com.mrl.pixiv.common.data.setting.AppearanceSettings
import com.mrl.pixiv.common.data.setting.ResolvedThemeColor
import com.mrl.pixiv.common.data.setting.resolve

/** 读取平台的壁纸配色，不支持时返回空值。 */
@Composable
expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?

/** 应用主题和样机共用同一套配色解析及纯黑背景规则。 */
@Composable
fun rememberAppColorScheme(appearance: AppearanceSettings, darkTheme: Boolean): ColorScheme {
    val platformScheme = platformDynamicColorScheme(darkTheme)
    val resolved = appearance.resolve(dynamicAvailable = platformScheme != null)
    return remember(resolved, darkTheme, appearance.amoledDark, platformScheme) {
        val scheme = when (resolved) {
            ResolvedThemeColor.Dynamic -> requireNotNull(platformScheme)
            ResolvedThemeColor.Baseline -> if (darkTheme) darkColorScheme() else expressiveLightColorScheme()
            is ResolvedThemeColor.Seeded -> previewColorScheme(resolved.seed, resolved.style, darkTheme)
        }
        if (darkTheme && appearance.amoledDark) scheme.toAmoled() else scheme
    }
}

fun previewColorScheme(seed: Long, style: AppPaletteStyle?, darkTheme: Boolean): ColorScheme =
    dynamicColorScheme(
        seedColor = Color(seed),
        isDark = darkTheme,
        isAmoled = false,
        style = when (style) {
            AppPaletteStyle.TONAL_SPOT -> PaletteStyle.TonalSpot
            AppPaletteStyle.NEUTRAL -> PaletteStyle.Neutral
            AppPaletteStyle.FIDELITY -> PaletteStyle.Fidelity
            null -> PaletteStyle.Monochrome
        },
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
    )

private fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
)
