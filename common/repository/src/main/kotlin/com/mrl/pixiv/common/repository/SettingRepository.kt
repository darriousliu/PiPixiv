package com.mrl.pixiv.common.repository

import android.os.Build
import com.mrl.pixiv.common.data.setting.SettingTheme
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.mmkv.MMKVUser
import com.mrl.pixiv.common.mmkv.asMutableStateFlow
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

val requireUserPreferenceValue: UserPreference
    get() = SettingRepository.userPreferenceFlow.value

val requireUserPreferenceFlow
    get() = SettingRepository.userPreferenceFlow

object SettingRepository : MMKVUser {
    private val userPreference by mmkvSerializable(UserPreference()).asMutableStateFlow()
    val userPreferenceFlow = userPreference.asStateFlow()

    val settingTheme
        get() = enumValueOf<SettingTheme>(
            userPreferenceFlow.value.theme.ifEmpty {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) SettingTheme.SYSTEM.toString() else SettingTheme.LIGHT.toString()
            }
        )

    fun setSettingTheme(theme: SettingTheme) = userPreference.update {
        it.copy(theme = theme.toString())
    }

    fun setEnableBypassSniffing(enable: Boolean) = userPreference.update {
        it.copy(enableBypassSniffing = enable)
    }

    fun setPictureSourceHost(host: String) = userPreference.update {
        it.copy(imageHost = host)
    }

    fun setHasShowBookmarkTip(hasShow: Boolean) = userPreference.update {
        it.copy(hasShowBookmarkTip = hasShow)
    }

    fun updateSettings(block: (UserPreference) -> UserPreference) {
        userPreference.update(block)
    }

    fun clear() {
        userPreference.value = UserPreference()
    }
}