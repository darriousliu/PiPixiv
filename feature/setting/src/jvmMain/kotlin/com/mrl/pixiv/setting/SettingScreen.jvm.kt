package com.mrl.pixiv.setting

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import java.util.Locale

actual fun getInitialLanguages(): String? {
    return Locale.getDefault().toLanguageTag()
}

actual fun triggerLocaleChange(
    currentLanguage: String,
    labelDefault: String
) {

}

@Composable
actual fun LazyListScope.AppLinkItem() {
}