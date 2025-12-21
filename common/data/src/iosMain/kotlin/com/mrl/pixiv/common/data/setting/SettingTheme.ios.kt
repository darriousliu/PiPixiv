package com.mrl.pixiv.common.data.setting

import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle

actual fun getAppCompatDelegateThemeMode(): SettingTheme {
    val style = UIApplication.sharedApplication.keyWindow?.overrideUserInterfaceStyle
    return when (style) {
        UIUserInterfaceStyle.UIUserInterfaceStyleLight -> SettingTheme.LIGHT
        UIUserInterfaceStyle.UIUserInterfaceStyleDark -> SettingTheme.DARK
        else -> SettingTheme.SYSTEM
    }
}

actual fun setAppCompatDelegateThemeMode(theme: SettingTheme) {
    val style = when (theme) {
        SettingTheme.LIGHT -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
        SettingTheme.DARK -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
        SettingTheme.SYSTEM -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
    }
    UIApplication.sharedApplication.keyWindow?.overrideUserInterfaceStyle = style
}
