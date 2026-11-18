package com.mrl.pixiv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntryDecorator
import com.mrl.pixiv.common.repository.IllustCacheRepo
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.NavigationRecord

/** Release picture sources only after the removed entry has finished its exit transition. */
@Composable
internal fun rememberPictureCacheDecorator(
    navigationManager: NavigationManager,
): NavEntryDecorator<NavigationRecord> {
    return remember(navigationManager) {
        val cacheKeys = mutableMapOf<Any, String>()
        NavEntryDecorator(
            onPop = { key ->
                // Newly opened pictures use their content key even if their UI never composed.
                val cacheKey = cacheKeys.remove(key) ?: key as? String
                if (cacheKey != null) {
                    val stillUsed = navigationManager.backStack.any {
                        (it.destination as? Destination.Picture)?.cacheKey == cacheKey
                    } || cacheKey in cacheKeys.values
                    if (!stillUsed) IllustCacheRepo.removeList(cacheKey)
                }
            },
        ) { entry ->
            val cacheKey = (entry.record.destination as? Destination.Picture)?.cacheKey
            DisposableEffect(entry.contentKey) {
                if (cacheKey != null) cacheKeys[entry.contentKey] = cacheKey
                onDispose { }
            }
            entry.Content()
        }
    }
}
