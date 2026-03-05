package com.mrl.pixiv.common.util

/**
 * Utility class to change the application's language in runtime.
 */
expect object LocaleHelper {
    fun getDisplayName(lang: String): String

    /**
     * Returns display name of a string language code.
     *
     * @param lang empty for system language
     */
    fun getLocalizedDisplayName(lang: String?): String
}
