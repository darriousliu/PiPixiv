package com.mrl.pixiv.setting

import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.LocaleHelper
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.label_default
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private val localeList = listOf(
    "en", "zh-CN", "zh-TW", "es", "hi", "pt", "ru", "ja", "de", "fr", "ar", "ko"
)

internal fun getLanguages(): ImmutableList<Language> {
    val langs = mutableListOf<Language>()
    localeList.forEach { langTag ->
        val displayName = LocaleHelper.getLocalizedDisplayName(langTag)
        if (displayName.isNotEmpty()) {
            langs.add(Language(langTag, displayName, null))
        }
    }

    langs.sortBy { it.displayName }
    langs.add(
        0,
        Language(
            AppUtil.getString(RStrings.label_default),
            AppUtil.getString(RStrings.label_default),
            null
        )
    )

    return langs.toImmutableList()
}

internal data class Language(
    val langTag: String,
    val displayName: String,
    val localizedDisplayName: String?,
)