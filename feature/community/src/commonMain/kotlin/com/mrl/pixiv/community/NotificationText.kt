package com.mrl.pixiv.community

import androidx.compose.ui.text.AnnotatedString
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString

internal fun notificationTextToAnnotatedString(html: String): AnnotatedString =
    htmlToAnnotatedString(html, compactMode = true)
