package com.mrl.pixiv.common.data.setting

import kotlinx.serialization.Serializable

@Serializable
data class HistorySettings(
    val enabled: Boolean = true,
    val autoClean: Boolean = true,
    val unlimited: Boolean = false,
    val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) {
    fun normalized(): HistorySettings {
        return copy(maxEntries = maxEntries.coerceIn(MIN_ENTRIES, MAX_ENTRIES))
    }

    companion object {
        const val MIN_ENTRIES = 5
        const val MAX_ENTRIES = 10_000
        const val DEFAULT_MAX_ENTRIES = 2_000
    }
}
