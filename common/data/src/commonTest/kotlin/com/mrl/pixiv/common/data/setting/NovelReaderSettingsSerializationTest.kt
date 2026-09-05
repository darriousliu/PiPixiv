package com.mrl.pixiv.common.data.setting

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
class NovelReaderSettingsSerializationTest {
    private val json = Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
    }
    private val protoBuf = ProtoBuf {
        encodeDefaults = false
    }

    @Test
    fun `reader settings normalize persisted values`() {
        assertEquals(
            NovelReaderSettings(
                fontSize = NovelReaderSettings.MAX_FONT_SIZE,
                lineSpacingSp = NovelReaderSettings.MIN_LINE_SPACING_SP,
            ),
            NovelReaderSettings(
                fontSize = Int.MAX_VALUE,
                lineSpacingSp = Int.MIN_VALUE,
            ).normalized(),
        )
    }

    @Test
    fun `reader settings round trip through user preference`() {
        val preference = UserPreference(
            novelReaderSettings = NovelReaderSettings(
                fontSize = 24,
                lineSpacingSp = 6,
            )
        )

        assertEquals(
            preference,
            json.decodeFromString<UserPreference>(json.encodeToString(preference)),
        )
        assertEquals(
            preference,
            protoBuf.decodeFromByteArray(
                UserPreference.serializer(),
                protoBuf.encodeToByteArray(UserPreference.serializer(), preference),
            ),
        )
    }

    @Test
    fun `legacy user preference uses default reader settings`() {
        val legacyBytes = protoBuf.encodeToByteArray(
            LegacyUserPreference.serializer(),
            LegacyUserPreference(
                appLanguage = "ja",
                theme = SettingTheme.DARK.name,
            ),
        )

        val decoded = protoBuf.decodeFromByteArray(
            UserPreference.serializer(),
            legacyBytes,
        )

        assertEquals("ja", decoded.appLanguage)
        assertEquals(SettingTheme.DARK.name, decoded.theme)
        assertEquals(NovelReaderSettings(), decoded.novelReaderSettings)
    }

    @Serializable
    private data class LegacyUserPreference(
        val appLanguage: String? = null,
        val theme: String = SettingTheme.SYSTEM.name,
    )
}
