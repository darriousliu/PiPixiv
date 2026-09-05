package com.mrl.pixiv.common.data.setting

import kotlinx.serialization.Serializable

@Serializable
data class BrowsingSettings(
    val previewImageQuality: PreviewImageQuality = PreviewImageQuality.MEDIUM,
    val autoHidePreviewControls: Boolean = true,
    val tapImageToOpenFullResolutionPreview: Boolean = true,
    val filterLongNovelTags: Boolean = false,
    val maxNovelTagLength: Int = DEFAULT_MAX_NOVEL_TAG_LENGTH,
    val maxNovelTagSegments: Int = DEFAULT_MAX_NOVEL_TAG_SEGMENTS,
    val searchResultIllustLayout: SearchResultIllustLayout = SearchResultIllustLayout.SQUARE,
) {
    companion object {
        const val MIN_NOVEL_TAG_LIMIT = 1
        const val MAX_NOVEL_TAG_LIMIT = 999
        const val DEFAULT_MAX_NOVEL_TAG_LENGTH = 30
        const val DEFAULT_MAX_NOVEL_TAG_SEGMENTS = 5
    }
}

@Serializable
enum class PreviewImageQuality {
    MEDIUM,
    HIGH,
    ORIGINAL,
}

@Serializable
enum class SearchResultIllustLayout {
    SQUARE,
    ORIGINAL_ASPECT_RATIO,
}
