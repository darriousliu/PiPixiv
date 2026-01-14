package com.mrl.pixiv.common.util

import androidx.appcompat.app.AppCompatDelegate
import com.mrl.pixiv.common.data.setting.SettingTheme

actual fun setAppCompatDelegateThemeMode(theme: SettingTheme) {
    AppCompatDelegate.setDefaultNightMode(
        when (theme) {
            SettingTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            SettingTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            SettingTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    )
}