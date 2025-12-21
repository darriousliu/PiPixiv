package com.mrl.pixiv.common.util

actual object ShareUtil {
    actual fun shareText(text: String) {
        copyToClipboard(text)
    }

    actual fun shareImage(imageUri: String) {
        copyImageToClipboard(imageUri)
    }
}