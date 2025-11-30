package com.mrl.pixiv.common.data.setting

import com.mrl.pixiv.common.data.Constants.IMAGE_HOST
import kotlinx.serialization.Serializable

@Serializable
data class UserPreference(
    val theme: String = SettingTheme.SYSTEM.name,
    val enableBypassSniffing: Boolean = false,
    val isR18Enabled: Boolean = false,
    val imageHost: String = IMAGE_HOST,
    val hasShowBookmarkTip: Boolean = false,
)
