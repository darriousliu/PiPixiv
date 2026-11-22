package com.mrl.pixiv.setting

import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.repository.AccountSessionManager
import com.mrl.pixiv.common.repository.AuthManager
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.recovery.DataRecoveryRepository
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.copyToClipboard
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.strings.copy_to_clipboard
import kotlinx.coroutines.Job
import org.koin.android.annotation.KoinViewModel

data object SettingState

sealed class SettingAction : ViewIntent {
    data object ExportToken : SettingAction()
}

@KoinViewModel
class SettingViewModel(
    private val sessions: AccountSessionManager,
    dataRecovery: DataRecoveryRepository,
) : BaseMviViewModel<SettingState, SettingAction>(
    initialState = SettingState,
) {
    val hasPendingRecovery = dataRecovery.hasPending
    private var logoutJob: Job? = null

    override suspend fun handleIntent(intent: SettingAction) {
        when (intent) {
            SettingAction.ExportToken -> {
                copyToClipboard(AuthManager.userRefreshToken)
                ToastUtil.safeShortToast(RStrings.copy_to_clipboard)
            }
        }
    }

    fun logout(deleteLocalData: Boolean) {
        if (logoutJob?.isActive == true) return
        val userId = AccountStore.activeUserId
        logoutJob = launchUI(onError = ::handleError) {
            sessions.remove(userId, deleteLocalData)
        }
    }

    fun savePictureSourceHost(host: String) {
        SettingRepository.setPictureSourceHost(host)
    }

    fun saveAppApiProxyHost(host: String) {
        SettingRepository.updateSettings {
            copy(appApiProxyHost = host)
        }
    }

    fun updateBypassSetting(setting: UserPreference.BypassSetting) {
        SettingRepository.updateSettings {
            copy(bypassSetting = setting)
        }
    }
}
