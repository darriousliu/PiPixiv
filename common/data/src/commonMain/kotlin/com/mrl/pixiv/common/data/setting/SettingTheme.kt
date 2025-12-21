package com.mrl.pixiv.common.data.setting

enum class SettingTheme {
    LIGHT, DARK, SYSTEM
}

expect fun getAppCompatDelegateThemeMode(): SettingTheme

expect fun setAppCompatDelegateThemeMode(theme: SettingTheme)