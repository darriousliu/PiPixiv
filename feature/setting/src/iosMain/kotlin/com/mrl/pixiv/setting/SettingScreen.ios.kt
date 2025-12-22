package com.mrl.pixiv.setting

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

actual fun getInitialLanguages(): String? {
    return NSLocale.preferredLanguages.firstOrNull() as? String
}

actual fun triggerLocaleChange(
    currentLanguage: String,
    labelDefault: String
) {
}

@Composable
actual fun LazyListScope.AppLinkItem() {
}