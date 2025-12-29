package com.mrl.pixiv.common.util

import platform.Photos.PHAsset

actual fun isImageExists(
    fileName: String,
    type: PictureType,
    subFolder: String?,
    fileUri: String
): Boolean {
    val count = PHAsset.fetchAssetsWithLocalIdentifiers(
        identifiers = listOf(fileUri.removePrefix("ph://")),
        options = null
    ).count
    return count > 0u
}