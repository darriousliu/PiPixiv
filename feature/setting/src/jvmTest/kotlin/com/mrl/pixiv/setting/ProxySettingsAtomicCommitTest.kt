package com.mrl.pixiv.setting

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
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
class ProxySettingsAtomicCommitTest {
    @Test
    fun appliesOnlyTheConfirmedValidProxyConfiguration() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
        try {
            runDesktopComposeUiTest(width = 390, height = 700, testTimeout = 30.seconds) {
                val updates = mutableListOf<BypassSetting>()
                var setting by mutableStateOf<BypassSetting>(BypassSetting.Direct)
                setContent {
                    MaterialExpressiveTheme {
                        BypassSettingEditor(
                            bypassSetting = setting,
                            supportsEch = true,
                            onUpdate = {
                                setting = it
                                updates += it
                            },
                        )
                    }
                }

                onNodeWithText("手动代理").performClick()
                onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("proxy.example.net")
                onAllNodes(hasSetTextAction()).onLast().performTextReplacement("0")
                onNodeWithText("确认").assertIsNotEnabled()
                onNodeWithText("取消").performClick()
                runOnIdle { assertEquals(emptyList(), updates) }

                onNodeWithText("手动代理").performClick()
                onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("proxy.example.net")
                onAllNodes(hasSetTextAction()).onLast().performTextReplacement("1080")
                onNodeWithText("SOCKS").performClick()
                runOnIdle { assertEquals(emptyList(), updates) }

                onNodeWithText("确认").performClick()
                runOnIdle {
                    assertEquals(
                        listOf<BypassSetting>(
                            BypassSetting.Proxy("proxy.example.net", 1080, BypassSetting.Proxy.ProxyType.SOCKS),
                        ),
                        updates,
                    )
                }
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
