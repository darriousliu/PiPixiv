package com.mrl.pixiv.common.data.setting

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceSettingsTest {
    @Test
    fun oldPreferencesKeepPlatformDefaultColors() {
        val appearance = Json.decodeFromString<UserPreference>("""{"theme":"DARK"}""").appearance

        assertEquals(AppearanceSettings(), appearance)
        assertEquals(ResolvedThemeColor.Dynamic, appearance.resolve(dynamicAvailable = true))
        assertEquals(ResolvedThemeColor.Baseline, appearance.resolve(dynamicAvailable = false))
    }

    @Test
    fun unavailableSourcesFallBackToPlatformDefault() {
        for (name in listOf(ThemeColor.CUSTOM.name, "FUTURE_COLOR")) {
            val appearance = AppearanceSettings(themeColor = name)
            assertEquals(ResolvedThemeColor.Dynamic, appearance.resolve(dynamicAvailable = true))
            assertEquals(ResolvedThemeColor.Baseline, appearance.resolve(dynamicAvailable = false))
        }
    }

    @Test
    fun unknownStyleFallsBackToTonalSpotAndInkUsesMonochrome() {
        val custom = AppearanceSettings(
            themeColor = ThemeColor.CUSTOM.name,
            customColor = 0xFF207585,
            paletteStyle = "FUTURE_STYLE",
        )
        assertEquals(
            ResolvedThemeColor.Seeded(ThemeColor.CUSTOM, 0xFF207585, AppPaletteStyle.TONAL_SPOT),
            custom.resolve(dynamicAvailable = true),
        )
        assertEquals(
            ResolvedThemeColor.Seeded(ThemeColor.INK, 0xFF6B7280, null),
            custom.copy(themeColor = ThemeColor.INK.name).resolve(dynamicAvailable = true),
        )
    }
}
