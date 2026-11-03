package com.mrl.pixiv.profile

import com.mrl.pixiv.common.data.setting.SettingTheme
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.UserManager
import com.mrl.pixiv.common.util.setAppCompatDelegateThemeMode
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import org.koin.android.annotation.KoinViewModel

data object ProfileState

sealed class ProfileAction : ViewIntent {
    data object GetUserInfo : ProfileAction()
}

@KoinViewModel
class ProfileViewModel : BaseMviViewModel<ProfileState, ProfileAction>(
    initialState = ProfileState,
) {
    override suspend fun handleIntent(intent: ProfileAction) {
        when (intent) {
            is ProfileAction.GetUserInfo -> getUserInfo()
        }
    }

    fun changeAppTheme(theme: SettingTheme) {
        SettingRepository.setSettingTheme(theme)
        setAppCompatDelegateThemeMode(theme)
    }

    private fun getUserInfo() {
        launchIO {
            UserManager.updateUserInfoAsync()
        }
    }
}

