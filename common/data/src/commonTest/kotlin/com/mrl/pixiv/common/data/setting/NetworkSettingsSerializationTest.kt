@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.mrl.pixiv.common.data.setting

import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
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
    fun legacyProtobufNoneRetainsCompatibility() {
        val bytes = ProtoBuf { encodeDefaults = true }
            .encodeToByteArray(LegacyPreference.serializer(), LegacyPreference())
        val preference = ProtoBuf.decodeFromByteArray(UserPreference.serializer(), bytes)
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
    fun legacySniProtobufMigratesWithoutWritingRemovedFieldsBack() {
        val protoBuf = ProtoBuf { encodeDefaults = true }
        for (savedUrl in listOf("https://doh.pub/dns-query", "https://dns.example/query")) {
            val legacy = LegacyPreference(
                bypassSetting = LegacyBypassSetting.Sni(
                    url = savedUrl,
                    fallback = mapOf("app-api.pixiv.net" to "210.140.139.155"),
                    nonStrictSSL = true,
                    dohTimeout = 99,
                ),
            )
            val restored = protoBuf.decodeFromByteArray(
                UserPreference.serializer(),
                protoBuf.encodeToByteArray(LegacyPreference.serializer(), legacy),
            )
            val expectedUrl = if (savedUrl == "https://doh.pub/dns-query") {
                BypassSetting.ECH.DEFAULT_DOH_URL
            } else {
                savedUrl
            }
            assertEquals(BypassSetting.ECH(expectedUrl), restored.bypassSetting)
            assertEquals("DARK", restored.theme)

            val saved = protoBuf.decodeFromByteArray(
                LegacyBypassSetting.Sni.serializer(),
                protoBuf.encodeToByteArray(
                    BypassSetting.ECH.serializer(),
                    restored.bypassSetting as BypassSetting.ECH,
                ),
            )
            // 使用旧结构读取新数据时，已移除的字段只能得到旧结构的默认值。
            assertEquals(LegacyBypassSetting.Sni(url = expectedUrl), saved)
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

    @Serializable
    @SerialName("sni")
    data class Sni(
        val url: String = "https://doh.pub/dns-query",
        val fallback: Map<String, String> = emptyMap(),
        val nonStrictSSL: Boolean = false,
        val dohTimeout: Int = 0,
    ) : LegacyBypassSetting
}
