package com.mrl.pixiv.common.compose.theme

import androidx.compose.ui.text.TextStyle

val TextStyle.tabularNumbers: TextStyle
    get() = copy(fontFeatureSettings = "tnum")
