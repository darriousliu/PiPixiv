package com.mrl.pixiv.common.util

import java.util.Locale

/**
 * Utility class to change the application's language in runtime.
 */
actual object LocaleHelper {
    actual fun getDisplayName(lang: String): String {
        val normalizedLang = when (lang) {
            "zh-CN" -> "zh-Hans"
            "zh-TW" -> "zh-Hant"
            else -> lang
        }

        return Locale.forLanguageTag(normalizedLang).displayName
    }

    /**
     * Returns display name of a string language code.
     *
     * @param lang empty for system language
     */
    actual fun getLocalizedDisplayName(lang: String?): String {
        if (lang == null) {
            return ""
        }

        val locale = when (lang) {
            "" -> Locale.getDefault()
            "zh-CN" -> Locale.forLanguageTag("zh-Hans")
            "zh-TW" -> Locale.forLanguageTag("zh-Hant")
            else -> Locale.forLanguageTag(lang)
        }
        return locale!!.getDisplayName(locale).replaceFirstChar { it.uppercase() }
    }
}
