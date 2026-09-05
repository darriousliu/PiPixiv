package com.mrl.pixiv.common.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

actual fun copyToClipboard(text: String) {
    // 复制到剪切板
    val clipboardManager = AppUtil.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboardManager?.setPrimaryClip(ClipData.newPlainText(text, text))
}

actual fun readTextFromClipboard(): String? = runCatching {
    val clipboardManager =
        AppUtil.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = clipboardManager?.primaryClip ?: return@runCatching null
    if (clip.itemCount == 0) return@runCatching null
    clip.getItemAt(0).coerceToText(AppUtil.appContext)?.toString()
}.getOrNull()
