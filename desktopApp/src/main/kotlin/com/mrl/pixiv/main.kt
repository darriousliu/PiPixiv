package com.mrl.pixiv

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.mrl.pixiv.common.analytics.FLAVOR
import com.mrl.pixiv.common.compose.LocalKeyEventFlow
import com.mrl.pixiv.common.network.ImageClient
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.BindDesktopWindowServices
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.di.Initialization
import com.mrl.pixiv.strings.app_name
import dev.nucleusframework.application.DecoratedWindow
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.window.NucleusDecoratedWindowTheme
import dev.nucleusframework.window.TitleBar
import dev.nucleusframework.window.styling.LocalTitleBarStyle
import io.github.vinceglb.filekit.FileKit
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import java.util.Locale

fun main(args: Array<String>) {
    if (runDesktopAotTrainingIfRequested()) return
    if ("--sentry-mapping-smoke-test" in args) {
        runDesktopSentryMappingSmokeTest()
        return
    }
    configureZoomImageMainThreadChecker()
    FileKit.init(appId = "PiPixiv")
    Initialization.initKoin()
    setDefaultLocale()
    AppUtil.init(PlatformContext.INSTANCE, FLAVOR)
    installTaoFatalErrorReporting()
    nucleusApplication(
        args = args,
        backend = NucleusBackend.Tao,
        enableSingleInstance = false,
        defaultLocale = Locale.getDefault(),
    ) {
        val appName = stringResource(RStrings.app_name)
        val preferences by SettingRepository.userPreferenceFlow.collectAsState()
        ProvideDesktopTheme(theme = preferences.theme) { darkTheme ->
            NucleusDecoratedWindowTheme(isDark = darkTheme) {
                CompositionLocalProvider(
                    LocalKeyEventFlow provides remember { MutableSharedFlow() }
                ) {
                    val flow = LocalKeyEventFlow.current as MutableSharedFlow
                    val scope = rememberCoroutineScope()
                    DecoratedWindow(
                        onCloseRequest = ::exitApplication,
                        title = appName,
                        onKeyEvent = {
                            scope.launch {
                                flow.emit(it)
                            }
                            true
                        }
                    ) {
                        BindDesktopWindowServices(checkNotNull(nucleusWindow.unsafe.taoWindow))
                        TitleBar { Text(appName, color = LocalTitleBarStyle.current.colors.content) }
                        val imageHttpClient = koinInject<HttpClient>(named<ImageClient>())

                        App(
                            darkTheme = darkTheme,
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
    }
}

private fun setDefaultLocale() {
    val (language, region) = SettingRepository.userPreferenceFlow.value.appLanguage?.split("-")
        ?.let { it[0] to it.getOrNull(1).orEmpty() } ?: return
    Locale.setDefault(Locale.of(language, region))
}
