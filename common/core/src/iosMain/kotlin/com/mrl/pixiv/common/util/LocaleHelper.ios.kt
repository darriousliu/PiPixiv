package com.mrl.pixiv.common.util

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleIdentifier
import platform.Foundation.currentLocale
import platform.Foundation.languageIdentifier
import platform.Foundation.localeWithLocaleIdentifier

actual object LocaleHelper {
    actual fun getDisplayName(lang: String): String {
        val normalizedLang = when (lang) {
            "zh-CN" -> "zh-Hans"
            "zh-TW" -> "zh-Hant"
            else -> lang
        }

        return NSLocale.currentLocale.displayNameForKey(NSLocaleIdentifier, normalizedLang)
            ?: normalizedLang
    }

    actual fun getLocalizedDisplayName(lang: String?): String {
        if (lang == null) {
            return ""
        }

        val targetLangIdentifier = when (lang) {
            "" -> NSLocale.currentLocale.languageIdentifier
            "zh-CN" -> "zh-Hans"
            "zh-TW" -> "zh-Hant"
            else -> lang
        }

        val targetLocale = NSLocale.localeWithLocaleIdentifier(targetLangIdentifier)
        val displayName = targetLocale.displayNameForKey(NSLocaleIdentifier, targetLangIdentifier)
            ?: targetLangIdentifier

        return displayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
