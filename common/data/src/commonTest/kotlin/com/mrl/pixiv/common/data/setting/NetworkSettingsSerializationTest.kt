@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.mrl.pixiv.common.data.setting

import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    fun legacyProtobufNoneRetainsCompatibility() {
        val bytes = ProtoBuf { encodeDefaults = true }
            .encodeToByteArray(LegacyPreference.serializer(), LegacyPreference())
        val preference = ProtoBuf.decodeFromByteArray(UserPreference.serializer(), bytes)
        assertEquals(BypassSetting.System, preference.bypassSetting)
        assertEquals("DARK", preference.theme)
    }

    @Test
    fun legacySniffingFlagStillEnablesSni() {
        val preference = Json.decodeFromString<UserPreference>("""{"enableBypassSniffing":true}""")
        assertIs<BypassSetting.SNI>(preference.bypassSetting)
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
    fun existingSniRetainsCustomSettings() {
        val preference = Json.decodeFromString<UserPreference>(
            """{"bypassSetting":{"type":"sni","url":"https://dns.example/query","nonStrictSSL":false,"dohTimeout":7,"fallback":{}}}""",
        )
        assertEquals(BypassSetting.SNI("https://dns.example/query", emptyMap(), false, 7), preference.bypassSetting)
    }

    @Test
    fun everyModeSurvivesPreferenceAndBackupRoundTrips() {
        val modes = listOf(
            BypassSetting.System,
            BypassSetting.Direct,
            BypassSetting.Proxy(),
            BypassSetting.Proxy("proxy.local", 1080, BypassSetting.Proxy.ProxyType.SOCKS),
            BypassSetting.SNI(),
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

@Serializable
private data class LegacyPreference(
    @ProtoNumber(2) val theme: String = "DARK",
    @ProtoNumber(4) val bypassSetting: LegacyBypassSetting = LegacyBypassSetting.None,
)

@Serializable
private sealed interface LegacyBypassSetting {
    @Serializable
    @SerialName("none")
    data object None : LegacyBypassSetting
}
