package com.mrl.pixiv.common.data.setting

import kotlinx.serialization.Serializable

@Serializable
data class BrowsingSettings(
    val previewImageQuality: PreviewImageQuality = PreviewImageQuality.MEDIUM,
    val autoHidePreviewControls: Boolean = true,
    val tapImageToOpenFullResolutionPreview: Boolean = true,
)

@Serializable
enum class PreviewImageQuality {
    MEDIUM,
    HIGH,
    ORIGINAL,
}
