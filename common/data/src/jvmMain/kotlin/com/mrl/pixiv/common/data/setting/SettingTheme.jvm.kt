package com.mrl.pixiv.common.data.setting

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal object JvmSettingTheme {
    private val _currentTheme = MutableStateFlow(SettingTheme.SYSTEM)
    val currentTheme: StateFlow<SettingTheme> = _currentTheme

    fun setCurrentTheme(theme: SettingTheme) {
        _currentTheme.value = theme
    }
}

actual fun getAppCompatDelegateThemeMode(): SettingTheme {
    return JvmSettingTheme.currentTheme.value
}

actual fun setAppCompatDelegateThemeMode(theme: SettingTheme) {
    JvmSettingTheme.setCurrentTheme(theme)
}
