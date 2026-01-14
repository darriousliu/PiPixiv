package com.mrl.pixiv.common.util

import android.content.Intent
import androidx.core.net.toUri

actual object ShareUtil {
    actual fun shareText(text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, text)
        val intent = Intent.createChooser(shareIntent, "Share")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        AppUtil.appContext.startActivity(intent)
    }

    actual fun shareImage(imageUri: String) {
        val uri = imageUri.toUri()
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(intent, "Share")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        AppUtil.appContext.startActivity(chooser)
    }
}