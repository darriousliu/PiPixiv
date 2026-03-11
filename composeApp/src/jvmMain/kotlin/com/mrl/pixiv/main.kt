package com.mrl.pixiv

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.mrl.pixiv.common.compose.LocalKeyEventFlow
import com.mrl.pixiv.common.data.setting.SettingTheme
import com.mrl.pixiv.common.network.ImageClient
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.di.Initialization
import com.mrl.pixiv.strings.app_name
import io.github.vinceglb.filekit.FileKit
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import java.util.Locale

fun main() {
    FileKit.init(appId = "PiPixiv")
    Initialization.initKoin()
    setDefaultLocale()
    application(exitProcessOnExit = false) {
        val appName = stringResource(RStrings.app_name)

        CompositionLocalProvider(
            LocalKeyEventFlow provides remember { MutableSharedFlow() }
        ) {
            val flow = LocalKeyEventFlow.current as MutableSharedFlow
            val scope = rememberCoroutineScope()
            Window(
                onCloseRequest = ::exitApplication,
                title = appName,
                onKeyEvent = {
                    scope.launch {
                        flow.emit(it)
                    }
                    true
                }
            ) {
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
        }
    }
}

private fun setDefaultLocale() {
    val (language, region) = SettingRepository.userPreferenceFlow.value.appLanguage?.split("-")
        ?.let { it[0] to it.getOrNull(1).orEmpty() } ?: return
    Locale.setDefault(Locale.of(language, region))
}