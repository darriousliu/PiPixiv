package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.mmkv.withAccountStorageLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

fun reloadAccountPreferences() = AccountPreferences.reload()

fun prefetchAccountPreferences() = AccountPreferences.prefetch()

private object AccountPreferences : KoinComponent {
    fun reload() = withAccountStorageLock {
        SettingRepository.reload()
        SearchRepository.reload()
        BookmarkedTagRepository.reload()
        get<NovelSeriesProgressRepository>().reload()
        UserManager.reload()
    }

    fun prefetch() {
        val userId = AccountStore.activeUserId
        if (userId <= 0) return
        SearchRepository.prefetch(userId)
        BookmarkedTagRepository.prefetch(userId)
        get<NovelSeriesProgressRepository>().prefetch(userId)
    }
}
