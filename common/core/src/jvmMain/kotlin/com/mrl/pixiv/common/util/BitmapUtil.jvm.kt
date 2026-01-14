package com.mrl.pixiv.common.util

import java.io.File
import java.net.URI

actual fun isImageExists(
    fileName: String,
    type: PictureType,
    subFolder: String?,
    fileUri: String
): Boolean {
    return File(URI(fileUri)).exists()
}