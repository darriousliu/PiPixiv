package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.mmkv.withAccountStorageLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

fun reloadAccountPreferences() = AccountPreferences.reload()

private object AccountPreferences : KoinComponent {
    fun reload() = withAccountStorageLock {
        SettingRepository.reload()
        SearchRepository.reload()
        BookmarkedTagRepository.reload()
        get<NovelSeriesProgressRepository>().reload()
        UserManager.reload()
    }
}
