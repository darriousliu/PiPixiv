package com.mrl.pixiv.common.data.setting

import kotlinx.serialization.Serializable

/** 按名称保存主题色与风格，以便读回来自其他版本的外观设置。 */
@Serializable
data class AppearanceSettings(
    val themeColor: String = ThemeColor.DYNAMIC.name,
    /** 最近一次应用的自定义种子色，格式为 0xFFRRGGBB。 */
    val customColor: Long? = null,
    val paletteStyle: String = AppPaletteStyle.TONAL_SPOT.name,
    val amoledDark: Boolean = false,
) {
    val resolvedPaletteStyle: AppPaletteStyle
        get() = AppPaletteStyle.entries.find { it.name == paletteStyle } ?: AppPaletteStyle.TONAL_SPOT
}

enum class ThemeColor(val seed: Long? = null) {
    DYNAMIC,
    DEFAULT,
    SKY(0xFF00B4FF),
    INDIGO(0xFF4F5BD5),
    WISTERIA(0xFF9C6ADE),
    SAKURA(0xFFE8508A),
    CORAL(0xFFE5603B),
    AMBER(0xFFE3A008),
    MATCHA(0xFF6FA33B),
    JADE(0xFF00A88A),
    INK(0xFF6B7280),
    CUSTOM,
}

enum class AppPaletteStyle { TONAL_SPOT, NEUTRAL, FIDELITY }

sealed interface ResolvedThemeColor {
    val color: ThemeColor

    data object Dynamic : ResolvedThemeColor {
        override val color = ThemeColor.DYNAMIC
    }

    data object Baseline : ResolvedThemeColor {
        override val color = ThemeColor.DEFAULT
    }

    /** 风格为空时使用「墨」的固定单色配色。 */
    data class Seeded(
        override val color: ThemeColor,
        val seed: Long,
        val style: AppPaletteStyle?,
    ) : ResolvedThemeColor
}

fun AppearanceSettings.resolve(dynamicAvailable: Boolean): ResolvedThemeColor {
    val color = ThemeColor.entries.find { it.name == themeColor } ?: ThemeColor.DYNAMIC
    if (color == ThemeColor.DEFAULT) return ResolvedThemeColor.Baseline
    val seed = if (color == ThemeColor.CUSTOM) customColor else color.seed
    if (seed == null) {
        return if (dynamicAvailable) ResolvedThemeColor.Dynamic else ResolvedThemeColor.Baseline
    }
    val style = if (color == ThemeColor.INK) null else resolvedPaletteStyle
    return ResolvedThemeColor.Seeded(color, seed, style)
}
