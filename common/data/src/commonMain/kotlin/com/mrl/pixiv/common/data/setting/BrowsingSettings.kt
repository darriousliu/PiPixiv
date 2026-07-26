package com.mrl.pixiv.common.data.setting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BrowsingSettings(
    val previewImageQuality: PreviewImageQuality = PreviewImageQuality.MEDIUM,
    val autoHidePreviewControls: Boolean = true,
    val tapImageToOpenFullResolutionPreview: Boolean = true,
    @SerialName("feedDisplayMode")
    val searchResultDisplayMode: SearchResultDisplayMode = SearchResultDisplayMode.INFINITE_SCROLL,
)

@Serializable
enum class PreviewImageQuality {
    MEDIUM,
    HIGH,
    ORIGINAL,
}

@Serializable
enum class SearchResultDisplayMode {
    INFINITE_SCROLL,
    PAGED,
}
