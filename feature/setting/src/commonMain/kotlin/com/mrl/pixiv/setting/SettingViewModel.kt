package com.mrl.pixiv.setting

import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.repository.AuthManager
import com.mrl.pixiv.common.repository.SearchRepository
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.UserManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.copyToClipboard
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.strings.copy_to_clipboard
import org.koin.android.annotation.KoinViewModel

data object SettingState

sealed class SettingAction : ViewIntent {
    data object ExportToken : SettingAction()
}

@KoinViewModel
class SettingViewModel : BaseMviViewModel<SettingState, SettingAction>(
    initialState = SettingState,
) {
    override suspend fun handleIntent(intent: SettingAction) {
        when (intent) {
            SettingAction.ExportToken -> {
                copyToClipboard(AuthManager.userRefreshToken)
                ToastUtil.safeShortToast(RStrings.copy_to_clipboard)
            }
        }
    }

    fun logout() {
        AuthManager.logout()
        UserManager.logout()
        SettingRepository.clear()
        SearchRepository.clear()
    }

    fun savePictureSourceHost(host: String) {
        SettingRepository.setPictureSourceHost(host)
    }

    fun updateBypassSetting(setting: UserPreference.BypassSetting) {
        SettingRepository.updateSettings {
            copy(bypassSetting = setting)
        }
    }
}
