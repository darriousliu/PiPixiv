package com.mrl.pixiv.common.data.setting

import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkSettingsSerializationTest {
    @Test
    fun newAndMissingSettingsDefaultToSystemProxy() {
        assertEquals(BypassSetting.System, UserPreference().bypassSetting)
        assertEquals(BypassSetting.System, Json.decodeFromString<UserPreference>("{}").bypassSetting)
    }

    @Test
    fun legacyNoneBecomesSystemProxyWithoutResettingOtherPreferences() {
        val preference = Json.decodeFromString<UserPreference>(
            """{"theme":"DARK","bypassSetting":{"type":"none"}}""",
        )
        assertEquals(BypassSetting.System, preference.bypassSetting)
        assertEquals("DARK", preference.theme)
    }

    @Test
    fun legacySniffingFlagEnablesEch() {
        val preference = Json.decodeFromString<UserPreference>("""{"enableBypassSniffing":true}""")
        assertEquals(BypassSetting.ECH(), preference.bypassSetting)
    }

    @Test
    fun explicitModeOverridesLegacySniffingFlag() {
        for (mode in listOf("none", "direct")) {
            val preference = Json.decodeFromString<UserPreference>(
                """{"enableBypassSniffing":true,"bypassSetting":{"type":"$mode"}}""",
            )
            val expected = if (mode == "none") BypassSetting.System else BypassSetting.Direct
            assertEquals(expected, preference.bypassSetting)
        }
    }

    @Test
    fun existingManualProxyRetainsHostPortAndProtocol() {
        val preference = Json.decodeFromString<UserPreference>(
            """{"bypassSetting":{"type":"proxy","host":"proxy.local","port":1081,"proxyType":"SOCKS"}}""",
        )
        assertEquals(
            BypassSetting.Proxy("proxy.local", 1081, BypassSetting.Proxy.ProxyType.SOCKS),
            preference.bypassSetting,
        )
    }

    @Test
    fun legacySniJsonMigratesToEchAndDiscardsRemovedSettings() {
        val savedUrls = mapOf(
            "https://doh.pub/dns-query" to BypassSetting.ECH.DEFAULT_DOH_URL,
            "http://dns.example/query" to BypassSetting.ECH.DEFAULT_DOH_URL,
            "https://dns.example/query" to "https://dns.example/query",
        )
        val backupJson = Json { encodeDefaults = true }
        for ((savedUrl, expectedUrl) in savedUrls) {
            val preference = Json.decodeFromString<UserPreference>(
                """{"theme":"DARK","bypassSetting":{"type":"sni","url":"$savedUrl","nonStrictSSL":true,"dohTimeout":99,"fallback":{"app-api.pixiv.net":"210.140.139.155"}}}""",
            )

            assertEquals(BypassSetting.ECH(expectedUrl), preference.bypassSetting)
            assertEquals("DARK", preference.theme)
            val saved = backupJson.parseToJsonElement(backupJson.encodeToString(preference))
                .jsonObject.getValue("bypassSetting").jsonObject
            assertEquals(setOf("type", "url"), saved.keys)
        }
    }

    @Test
    fun everyModeSurvivesPreferenceAndBackupRoundTrips() {
        val modes = listOf(
            BypassSetting.System,
            BypassSetting.Direct,
            BypassSetting.Proxy(),
            BypassSetting.Proxy("proxy.local", 1080, BypassSetting.Proxy.ProxyType.SOCKS),
            BypassSetting.ECH(),
            BypassSetting.ECH("https://dns.example/query"),
        )
        for (encodeDefaults in listOf(false, true)) {
            val json = Json { this.encodeDefaults = encodeDefaults }
            for (mode in modes) {
                val preference = UserPreference(bypassSetting = mode)
                assertEquals(preference, json.decodeFromString<UserPreference>(json.encodeToString(preference)))
            }
        }
    }
}
