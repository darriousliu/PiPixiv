package com.mrl.pixiv.profile

import com.mrl.pixiv.common.data.setting.SettingTheme
import com.mrl.pixiv.common.repository.AuthManager
import com.mrl.pixiv.common.repository.SearchRepository
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.UserManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.copyToClipboard
import com.mrl.pixiv.common.util.setAppCompatDelegateThemeMode
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.strings.copy_to_clipboard
import org.koin.android.annotation.KoinViewModel

data object ProfileState

sealed class ProfileAction : ViewIntent {
    data object GetUserInfo : ProfileAction()
    data object ExportToken : ProfileAction()
}

@KoinViewModel
class ProfileViewModel : BaseMviViewModel<ProfileState, ProfileAction>(
    initialState = ProfileState,
) {
    override suspend fun handleIntent(intent: ProfileAction) {
        when (intent) {
            is ProfileAction.GetUserInfo -> getUserInfo()
            is ProfileAction.ExportToken -> exportToken()
        }
    }

    private fun exportToken() {
        val token = AuthManager.userRefreshToken
        copyToClipboard(token)
        ToastUtil.safeShortToast(RStrings.copy_to_clipboard)
    }

    fun logout() {
        AuthManager.logout()
        UserManager.logout()
        SettingRepository.clear()
        SearchRepository.clear()
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


