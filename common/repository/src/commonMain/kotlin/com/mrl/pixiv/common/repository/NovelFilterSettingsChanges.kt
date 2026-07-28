package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.setting.BrowsingSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal object NovelFilterSettingsChanges {
    private val invalidators = MutableStateFlow<Set<() -> Unit>>(emptySet())

    fun notifyIfChanged(
        previous: BrowsingSettings,
        current: BrowsingSettings,
    ) {
        if (!previous.hasDifferentNovelFilterSettings(current)) return
        invalidators.value.toList().forEach { it() }
    }

    fun register(invalidator: () -> Unit): () -> Unit {
        invalidators.update { it + invalidator }
        return {
            invalidators.update { it - invalidator }
        }
    }
}

fun BrowsingSettings.hasDifferentNovelFilterSettings(
    other: BrowsingSettings,
): Boolean = filterLongNovelTags != other.filterLongNovelTags ||
        maxNovelTagLength != other.maxNovelTagLength ||
        maxNovelTagSegments != other.maxNovelTagSegments
