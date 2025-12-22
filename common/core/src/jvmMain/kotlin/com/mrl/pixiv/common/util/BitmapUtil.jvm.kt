package com.mrl.pixiv.common.util

actual fun isImageExists(
    fileName: String,
    type: PictureType,
    subFolder: String?
): Boolean {
    return false
}