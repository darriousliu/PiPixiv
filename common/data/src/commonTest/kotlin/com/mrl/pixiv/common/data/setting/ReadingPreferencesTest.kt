package com.mrl.pixiv.common.data.setting

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingPreferencesTest {
    @Test
    fun existingPreferencesStartWithReadLater() {
        val restored = Json.decodeFromString<UserPreference>("""{"theme":"DARK"}""")
        assertEquals(ReadingTab.READ_LATER, restored.readingTab)
        assertEquals("DARK", restored.theme)
    }

}
