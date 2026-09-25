@file:Suppress("DEPRECATION")
@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package com.mrl.pixiv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import com.mrl.pixiv.common.data.setting.SettingTheme
import dev.nucleusframework.darkmodedetector.isSystemInDarkMode

@Composable
internal fun ProvideDesktopTheme(
    theme: String,
    systemDarkTheme: Boolean = isSystemInDarkMode(),
    content: @Composable (darkTheme: Boolean) -> Unit,
) {
    val darkTheme = when (theme) {
        SettingTheme.LIGHT.name -> false
        SettingTheme.DARK.name -> true
        else -> systemDarkTheme
    }
    // Nucleus 2.5.18 写入 Skiko 枚举，而当前 Compose 仍按自己的枚举判断深色。
    // 覆盖上层的错误类型，并统一手动模式下窗口与内部组件使用的主题。
    CompositionLocalProvider(
        LocalSystemTheme provides if (darkTheme) SystemTheme.Dark else SystemTheme.Light,
    ) {
        content(darkTheme)
    }
}
