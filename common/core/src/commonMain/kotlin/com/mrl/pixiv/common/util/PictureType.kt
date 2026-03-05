package com.mrl.pixiv.common.util

enum class PictureType(
    val extension: String,
    val mimeType: String,
) {
    PNG(".png", "image/png"),
    JPG(".jpg", "image/jpeg"),
    JPEG(".jpeg", "image/jpeg"),
    GIF(".gif", "image/gif");

    companion object {
        fun fromMimeType(mimeType: String?): PictureType? {
            return entries.find { it.mimeType == mimeType?.lowercase() }
        }
    }
}
