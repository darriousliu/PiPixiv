package com.mrl.pixiv.common.data.setting

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
class BrowsingSettingsSerializationTest {
    private val json = Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
    }
    private val protoBuf = ProtoBuf {
        encodeDefaults = false
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
    fun `old protobuf settings use square search result layout`() {
        val legacyBytes = protoBuf.encodeToByteArray(
            LegacyBrowsingSettings.serializer(),
            LegacyBrowsingSettings(
                previewImageQuality = PreviewImageQuality.ORIGINAL,
                autoHidePreviewControls = false,
                maxNovelTagLength = 48,
            ),
        )

        val decoded = protoBuf.decodeFromByteArray(
            BrowsingSettings.serializer(),
            legacyBytes,
        )

        assertEquals(PreviewImageQuality.ORIGINAL, decoded.previewImageQuality)
        assertEquals(false, decoded.autoHidePreviewControls)
        assertEquals(48, decoded.maxNovelTagLength)
        assertEquals(SearchResultIllustLayout.SQUARE, decoded.searchResultIllustLayout)
    }

    @Test
    fun `original aspect ratio layout round trips through json and protobuf`() {
        val settings = BrowsingSettings(
            searchResultIllustLayout = SearchResultIllustLayout.ORIGINAL_ASPECT_RATIO,
        )

        assertEquals(
            settings,
            json.decodeFromString<BrowsingSettings>(json.encodeToString(settings)),
        )
        assertEquals(
            settings,
            protoBuf.decodeFromByteArray(
                BrowsingSettings.serializer(),
                protoBuf.encodeToByteArray(BrowsingSettings.serializer(), settings),
            ),
        )
    }

    @Serializable
    private data class LegacyBrowsingSettings(
        val previewImageQuality: PreviewImageQuality = PreviewImageQuality.MEDIUM,
        val autoHidePreviewControls: Boolean = true,
        val tapImageToOpenFullResolutionPreview: Boolean = true,
        val filterLongNovelTags: Boolean = false,
        val maxNovelTagLength: Int = BrowsingSettings.DEFAULT_MAX_NOVEL_TAG_LENGTH,
        val maxNovelTagSegments: Int = BrowsingSettings.DEFAULT_MAX_NOVEL_TAG_SEGMENTS,
    )
}
