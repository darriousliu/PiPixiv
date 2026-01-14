package com.mrl.pixiv.setting

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import com.mrl.pixiv.common.repository.SettingRepository
import java.util.Locale

actual fun getInitialLanguages(): String? {
    return SettingRepository.userPreferenceFlow.value.appLanguage
}

actual fun triggerLocaleChange(
    currentLanguage: String,
    labelDefault: String
) {
    var finalLang: String? = currentLanguage
    if (currentLanguage == labelDefault) {
        finalLang = null
    }
    val (lang, region) = finalLang?.split("-")?.let { it[0] to it.getOrNull(1).orEmpty() }
        ?: ("" to "")
    Locale.setDefault(finalLang?.let { Locale.of(lang, region) } ?: Locale.getDefault())
    SettingRepository.updateSettings {
        copy(appLanguage = finalLang)
    }
}

@Composable
actual fun LazyListScope.AppLinkItem() {
}