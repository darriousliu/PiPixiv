package com.mrl.pixiv.common.data.setting

import kotlinx.serialization.Serializable

@Serializable
data class BrowsingSettings(
    val previewImageQuality: PreviewImageQuality = PreviewImageQuality.MEDIUM,
    val autoHidePreviewControls: Boolean = true,
    val tapImageToOpenFullResolutionPreview: Boolean = true,
    val feedDisplayMode: FeedDisplayMode = FeedDisplayMode.INFINITE_SCROLL,
)

@Serializable
enum class PreviewImageQuality {
    MEDIUM,
    HIGH,
    ORIGINAL,
}

@Serializable
enum class FeedDisplayMode {
    INFINITE_SCROLL,
    PAGED,
}
