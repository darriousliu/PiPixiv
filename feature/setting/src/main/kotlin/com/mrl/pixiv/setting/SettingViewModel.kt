package com.mrl.pixiv.setting

import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import org.koin.android.annotation.KoinViewModel

data object SettingState

sealed class SettingAction : ViewIntent {
    data object SwitchBypassSniffing : SettingAction()
    data class SavePictureSourceHost(val host: String) : SettingAction()
}

@KoinViewModel
class SettingViewModel : BaseMviViewModel<SettingState, SettingAction>(
    initialState = SettingState,
) {
    override suspend fun handleIntent(intent: SettingAction) {
        when (intent) {
            is SettingAction.SwitchBypassSniffing -> switchBypassSniffing()
            is SettingAction.SavePictureSourceHost -> savePictureSourceHost(intent.host)
        }
    }

    private fun savePictureSourceHost(host: String) {
        SettingRepository.setPictureSourceHost(host)
    }

    private fun switchBypassSniffing() {
        SettingRepository.setEnableBypassSniffing(!requireUserPreferenceValue.enableBypassSniffing)
    }
}