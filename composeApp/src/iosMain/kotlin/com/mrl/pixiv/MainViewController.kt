package com.mrl.pixiv

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.mrl.pixiv.common.data.setting.SettingTheme
import com.mrl.pixiv.common.network.ImageClient
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import io.ktor.client.HttpClient
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

fun MainViewController() = ComposeUIViewController {
    val imageHttpClient = koinInject<HttpClient>(named<ImageClient>())
    val theme by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle { theme }
    App(
        darkTheme = when (theme) {
            SettingTheme.LIGHT.name -> false
            SettingTheme.DARK.name -> true
            SettingTheme.SYSTEM.name -> isSystemInDarkTheme()
            else -> isSystemInDarkTheme()
        },
        imageLoaderBuilder = {
            this.components {
                add(KtorNetworkFetcherFactory(imageHttpClient))
            }
        }
    )
}