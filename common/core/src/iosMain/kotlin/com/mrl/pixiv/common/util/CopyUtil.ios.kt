package com.mrl.pixiv.common.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.UIKit.UIPasteboard
import platform.UIKit.UIPasteboardDetectionPatternLink
import platform.UIKit.UIPasteboardDetectionPatternProbableWebURL
import kotlin.coroutines.resume

actual suspend fun copyToClipboard(text: String) {
    withContext(Dispatchers.IO) {
        UIPasteboard.generalPasteboard.string = text
    }
}

actual suspend fun readTextFromClipboard(): String? = withContext(Dispatchers.IO) {
    val pasteboard = UIPasteboard.generalPasteboard
    if (!pasteboard.hasStrings && !pasteboard.hasURLs) return@withContext null
    pasteboard.string ?: pasteboard.URL?.absoluteString
}

actual suspend fun clipboardChangeCount(): Long? = withContext(Dispatchers.IO) {
    UIPasteboard.generalPasteboard.changeCount.toLong()
}

actual suspend fun readLinkTextFromClipboard(): String? = withContext(Dispatchers.IO) {
    val pasteboard = UIPasteboard.generalPasteboard
    if (!pasteboard.hasStrings && !pasteboard.hasURLs) return@withContext null
    if (!pasteboard.containsLinkPattern()) return@withContext null
    pasteboard.string ?: pasteboard.URL?.absoluteString
}

private suspend fun UIPasteboard.containsLinkPattern(): Boolean =
    suspendCancellableCoroutine { continuation ->
        // 先通过系统无权限弹窗模式探测剪贴板是否包含链接，避免每次切回搜索页都阻塞或触发粘贴弹窗。
        val patterns = setOf(
            UIPasteboardDetectionPatternProbableWebURL,
            UIPasteboardDetectionPatternLink,
        )
        detectPatternsForPatterns(patterns) { detectedPatterns, error ->
            if (!continuation.isActive) return@detectPatternsForPatterns
            val hasLink = error == null && !detectedPatterns.isNullOrEmpty()
            continuation.resume(hasLink)
        }
    }
