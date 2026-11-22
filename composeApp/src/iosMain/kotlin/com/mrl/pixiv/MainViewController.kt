package com.mrl.pixiv

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.window.ComposeUIViewController
import coil3.PlatformContext
import com.mrl.pixiv.common.analytics.FLAVOR
import com.mrl.pixiv.common.compose.LocalKeyEventFlow
import com.mrl.pixiv.common.data.setting.SettingTheme
import com.mrl.pixiv.common.network.ImageClient
import com.mrl.pixiv.common.network.RoutedHttpClient
import com.mrl.pixiv.common.network.imageNetworkFetcher
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.LocalStatusBarVisibilityController
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

fun MainViewController(onStatusBarVisibilityChange: (Boolean) -> Unit) = run {
    val keyStateFlow = MutableSharedFlow<KeyEvent>()
    AppUtil.init(PlatformContext.INSTANCE, FLAVOR)
    ComposeUIViewController {
        LaunchedEffect(Unit) {
            IosBackgroundDownloads.reconnectPending()
        }
        CompositionLocalProvider(
            LocalKeyEventFlow provides keyStateFlow,
            LocalStatusBarVisibilityController provides onStatusBarVisibilityChange,
        ) {
            val imageHttpClient = koinInject<RoutedHttpClient<HttpClient>>(named<ImageClient>())
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
                        add(imageNetworkFetcher(imageHttpClient))
                    }
                }
            )
        }
    }
}
