package com.mrl.pixiv.setting

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import com.mrl.pixiv.common.repository.SettingRepository
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

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
    SettingRepository.updateSettings {
        copy(appLanguage = finalLang)
    }
}

@Composable
actual fun LazyListScope.AppLinkItem() {
}

/**
 * 获取应用当前的语言代码。
 *
 * @return 返回当前应用的语言代码（例如 "en"），如果无法获取则返回 null。
 */
private fun appCurrentLanguage(): String? {
    return (NSLocale.preferredLanguages.firstOrNull() as String?)?.substringBeforeLast('-')
}