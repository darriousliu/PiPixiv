package com.mrl.pixiv.common.util

expect fun copyToClipboard(text: String)

expect fun readTextFromClipboard(): String?
