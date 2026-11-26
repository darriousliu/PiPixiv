package com.mrl.pixiv.common.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

actual suspend fun copyToClipboard(text: String) {
    // 复制到剪切板
    val clipboardManager = AppUtil.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    // 可空的 getter 和非空的 setter 让 Kotlin 将 primaryClip 识别为只读属性。
    @Suppress("UsePropertyAccessSyntax")
    clipboardManager?.setPrimaryClip(ClipData.newPlainText(text, text))
}

actual suspend fun readTextFromClipboard(): String? = runCatching {
    val clipboardManager =
        AppUtil.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = clipboardManager?.primaryClip ?: return@runCatching null
    if (clip.itemCount == 0) return@runCatching null
    clip.getItemAt(0).coerceToText(AppUtil.appContext)?.toString()
}.getOrNull()

actual suspend fun readLinkTextFromClipboard(): String? = readTextFromClipboard()

actual suspend fun clipboardChangeCount(): Long? = null
