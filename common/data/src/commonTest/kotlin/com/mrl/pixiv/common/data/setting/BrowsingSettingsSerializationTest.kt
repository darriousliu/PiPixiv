package com.mrl.pixiv.common.data.setting

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class BrowsingSettingsSerializationTest {
    private val json = Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
    }

    @Test
    fun `old json settings use square search result layout`() {
        val decoded = json.decodeFromString<BrowsingSettings>(
            """
                {
                  "previewImageQuality": "HIGH",
                  "autoHidePreviewControls": false
                }
            """.trimIndent()
        )

        assertEquals(PreviewImageQuality.HIGH, decoded.previewImageQuality)
        assertEquals(SearchResultIllustLayout.SQUARE, decoded.searchResultIllustLayout)
    }

    @Test
    fun `original aspect ratio layout round trips through json`() {
        val settings = BrowsingSettings(
            searchResultIllustLayout = SearchResultIllustLayout.ORIGINAL_ASPECT_RATIO,
        )

        assertEquals(
            settings,
            json.decodeFromString<BrowsingSettings>(json.encodeToString(settings)),
        )
    }
}
