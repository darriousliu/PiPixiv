package com.mrl.pixiv.common.data.setting

import androidx.appcompat.app.AppCompatDelegate

enum class SettingTheme {
    LIGHT, DARK, SYSTEM
}

fun getAppCompatDelegateThemeMode(): SettingTheme {
    return when (AppCompatDelegate.getDefaultNightMode()) {
        AppCompatDelegate.MODE_NIGHT_NO -> SettingTheme.LIGHT
        AppCompatDelegate.MODE_NIGHT_YES -> SettingTheme.DARK
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> SettingTheme.SYSTEM
        else -> SettingTheme.SYSTEM
    }
}

fun setAppCompatDelegateThemeMode(theme: SettingTheme) {
    AppCompatDelegate.setDefaultNightMode(
        when (theme) {
            SettingTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            SettingTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            SettingTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    )
}