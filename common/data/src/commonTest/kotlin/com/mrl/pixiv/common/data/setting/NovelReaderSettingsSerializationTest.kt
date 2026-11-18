package com.mrl.pixiv.common.data.setting

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class NovelReaderSettingsSerializationTest {
    private val json = Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
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
    }

    @Test
    fun `old json user preference uses default reader settings`() {
        val decoded = json.decodeFromString<UserPreference>(
            """{"appLanguage":"ja","theme":"DARK"}""",
        )

        assertEquals("ja", decoded.appLanguage)
        assertEquals(SettingTheme.DARK.name, decoded.theme)
        assertEquals(NovelReaderSettings(), decoded.novelReaderSettings)
    }
}
