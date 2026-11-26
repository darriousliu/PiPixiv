package com.mrl.pixiv.common.util

expect suspend fun copyToClipboard(text: String)

expect suspend fun readTextFromClipboard(): String?

expect suspend fun readLinkTextFromClipboard(): String?

expect suspend fun clipboardChangeCount(): Long?
