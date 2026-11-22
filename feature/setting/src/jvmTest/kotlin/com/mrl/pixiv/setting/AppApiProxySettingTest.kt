package com.mrl.pixiv.setting

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mrl.pixiv.setting.network.components.AppApiProxySetting
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class AppApiProxySettingTest {
    @Test
    fun savesOnlyTheConfirmedHostAndCanRestoreTheDefault() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
        try {
            runDesktopComposeUiTest(width = 390, height = 700, testTimeout = 30.seconds) {
                val updates = mutableListOf<String>()
                var proxyHost by mutableStateOf("")
                setContent {
                    MaterialExpressiveTheme {
                        key(proxyHost) {
                            AppApiProxySetting(
                                proxyHost = proxyHost,
                                onSave = {
                                    proxyHost = it
                                    updates += it
                                },
                            )
                        }
                    }
                }

                onNodeWithText("app-api.pixiv.net").performClick()
                onNodeWithText("自定义域名").performClick()
                onNode(hasSetTextAction()).performTextReplacement("http://proxy.example.net")
                onNodeWithText("保存").assertIsNotEnabled()
                onNode(hasSetTextAction()).performTextReplacement("PROXY.example.net.")
                runOnIdle { assertEquals(emptyList(), updates) }

                onNodeWithText("保存").performClick()
                runOnIdle { assertEquals(listOf("proxy.example.net"), updates) }

                onNodeWithText("proxy.example.net").performClick()
                onNodeWithText("默认 · app-api.pixiv.net").performClick()
                runOnIdle { assertEquals(listOf("proxy.example.net", ""), updates) }
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
