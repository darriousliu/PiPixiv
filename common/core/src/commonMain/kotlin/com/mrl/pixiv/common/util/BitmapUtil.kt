package com.mrl.pixiv.common.util

expect fun isImageExists(
    fileName: String,
    type: PictureType,
    subFolder: String? = null,
    fileUri: String = "",
): Boolean