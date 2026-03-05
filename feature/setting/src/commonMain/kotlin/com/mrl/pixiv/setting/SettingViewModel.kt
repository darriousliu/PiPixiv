package com.mrl.pixiv.setting

import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import org.koin.android.annotation.KoinViewModel

data object SettingState

sealed class SettingAction : ViewIntent

@KoinViewModel
class SettingViewModel : BaseMviViewModel<SettingState, SettingAction>(
    initialState = SettingState,
) {
    override suspend fun handleIntent(intent: SettingAction) {
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
