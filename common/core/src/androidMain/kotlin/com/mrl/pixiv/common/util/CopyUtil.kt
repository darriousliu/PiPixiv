package com.mrl.pixiv.common.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

fun copyToClipboard(text: String) {
    // 复制到剪切板
    val clipboardManager = AppUtil.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboardManager?.setPrimaryClip(ClipData.newPlainText(text, text))
}