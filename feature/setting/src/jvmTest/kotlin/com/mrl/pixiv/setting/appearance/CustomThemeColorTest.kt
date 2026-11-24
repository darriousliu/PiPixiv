package com.mrl.pixiv.setting.appearance

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mrl.pixiv.common.data.setting.AppPaletteStyle
import com.mrl.pixiv.common.data.setting.AppearanceSettings
import com.mrl.pixiv.common.data.setting.SettingTheme
import com.mrl.pixiv.common.data.setting.ThemeColor
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class CustomThemeColorTest {
    @Test
    fun onlyApplyPersistsTheCustomSeedAndStyle() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
        try {
            runDesktopComposeUiTest(width = 840, height = 720, testTimeout = 30.seconds) {
                val initial = AppearanceSettings(themeColor = ThemeColor.SKY.name)
                val writes = mutableListOf<AppearanceSettings>()
                setContent {
                    MaterialExpressiveTheme {
                        var pickerSeed by remember { mutableStateOf<Long?>(null) }
                        AppearanceSettingContent(
                            appearance = initial,
                            theme = SettingTheme.LIGHT,
                            darkTheme = false,
                            useLiquidGlassNavigation = true,
                            spanCount = 2,
                            pickerSeed = pickerSeed,
                            onPickerSeedChange = { pickerSeed = it },
                            onAppearanceChange = { writes += it },
                            onThemeChange = {},
                            onGlassChange = {},
                            onBack = {},
                        )
                    }
                }

                onNodeWithText("自定义").performScrollTo().performClick()
                onNode(hasSetTextAction()).performTextReplacement("#AABBCC")
                onNodeWithText("改用原色").performClick()
                onNodeWithText("取消").performClick()
                runOnIdle { assertEquals(emptyList(), writes) }

                onNodeWithText("自定义").performScrollTo().performClick()
                onNode(hasSetTextAction()).performTextReplacement("#2468")
                onNodeWithText("应用").assertIsNotEnabled()
                onNode(hasSetTextAction()).performTextReplacement("#2468AC")
                onNodeWithText("改用原色").performClick()
                onNodeWithText("应用").performClick()
                runOnIdle {
                    assertEquals(
                        listOf(
                            initial.copy(
                                themeColor = ThemeColor.CUSTOM.name,
                                customColor = 0xFF2468AC,
                                paletteStyle = AppPaletteStyle.FIDELITY.name,
                            ),
                        ),
                        writes,
                    )
                }
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
