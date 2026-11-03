package com.mrl.pixiv.setting

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import com.mrl.pixiv.setting.network.components.BypassSettingEditor
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class EchSettingsRestorationTest {
    @Test
    fun advancedSettingsAndDohDraftSurviveRestorationBeforeSaving() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
        try {
            runDesktopComposeUiTest(width = 390, height = 700, testTimeout = 30.seconds) {
                val savedUrl = "https://dns.example/custom-resolve"
                val draft = "  $savedUrl  "
                val updates = mutableListOf<BypassSetting>()
                var setting by mutableStateOf<BypassSetting>(BypassSetting.ECH())
                var registry by mutableStateOf(SaveableStateRegistry(null) { true })
                var showContent by mutableStateOf(true)
                setContent {
                    if (showContent) {
                        CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
                            MaterialExpressiveTheme {
                                BypassSettingEditor(
                                    bypassSetting = setting,
                                    onUpdate = {
                                        setting = it
                                        updates += it
                                    },
                                )
                            }
                        }
                    }
                }

                onNodeWithText("DoH 地址").assertDoesNotExist()
                onNodeWithText("高级设置").performClick()
                onNodeWithText("DoH 地址").performClick()
                onNode(hasSetTextAction()).performTextReplacement(draft)

                // Desktop 的 StateRestorationTester 尚未实现，直接保存注册表并重建组件。
                lateinit var savedState: Map<String, List<Any?>>
                runOnIdle {
                    savedState = registry.performSave()
                    showContent = false
                }
                runOnIdle {
                    registry = SaveableStateRegistry(savedState) { true }
                    showContent = true
                }

                onNode(hasSetTextAction()).assertIsDisplayed().assertTextEquals(draft)
                onNodeWithText("确认").performClick()
                onNodeWithText(savedUrl).assertIsDisplayed()
                runOnIdle { assertEquals(listOf<BypassSetting>(BypassSetting.ECH(savedUrl)), updates) }
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
