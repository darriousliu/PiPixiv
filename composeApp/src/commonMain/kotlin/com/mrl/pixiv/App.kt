package com.mrl.pixiv

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Severity
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import com.mrl.pixiv.common.compose.theme.rememberAppColorScheme
import com.mrl.pixiv.common.compose.ui.LocalScrollbarStyle
import com.mrl.pixiv.common.compose.ui.defaultScrollbarStyle
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.VersionManager
import com.mrl.pixiv.common.util.isDesktop
import com.mrl.pixiv.common.util.platform
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.navigation.Navigation3MainGraph
import com.mrl.pixiv.setting.network.AiLocalNetworkPermissionEffect
import com.mrl.pixiv.setting.network.LocalNetworkPermissionEffect
import com.mrl.pixiv.splash.MigrationSplash
import com.mrl.pixiv.splash.SplashViewModel
import com.mrl.pixiv.theme.PiPixivTheme
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.resolve
import io.github.vinceglb.filekit.toKotlinxIoPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import okio.Path.Companion.toPath
import org.koin.compose.viewmodel.koinViewModel
import co.touchlab.kermit.Logger as KermitLogger
import coil3.util.Logger as CoilLogger
import coil3.util.Logger.Level as CoilLogLevel

@Composable
fun App(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    imageLoaderBuilder: ImageLoader.Builder.() -> Unit = {},
    splashViewModel: SplashViewModel = koinViewModel()
) {
    val appearance by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle { appearance }
    val colorScheme = rememberAppColorScheme(appearance, darkTheme)
    val appLanguage by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle { appLanguage }
    val aiEndpoint by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle {
        aiTranslationConfig.endpoint
    }
    val bypassSetting by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle {
        bypassSetting
    }
    val scrollbarStyle = remember(colorScheme) {
        defaultScrollbarStyle().copy(
            unhoverColor = if (platform.isDesktop()) colorScheme.primary.copy(alpha = 0.364f) else Color.Transparent,
            hoverColor = colorScheme.primary
        )
    }

    SetUpImageLoaderFactory(imageLoaderBuilder)
    LocalNetworkPermissionEffect(bypassSetting)
    AiLocalNetworkPermissionEffect(aiEndpoint)

    LaunchedEffect(Unit) {
        VersionManager.checkUpdate()
    }

    CompositionLocalProvider(LocalScrollbarStyle provides scrollbarStyle) {
        key(appLanguage) {
            PiPixivTheme(
                darkTheme = darkTheme,
                colorScheme = colorScheme
            ) {
                val state = splashViewModel.asState()
                val destination = state.startDestination
                if (destination == null) {
                    val migrationState by splashViewModel.migrationState.collectAsStateWithLifecycle()
                    MigrationSplash(migrationState, splashViewModel, modifier)
                } else {
                    Navigation3MainGraph(
                        startDestination = destination,
                        modifier = modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun SetUpImageLoaderFactory(imageLoaderBuilder: ImageLoader.Builder.() -> Unit) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .diskCache(CoilDiskCache.get())
            .memoryCache(CoilMemoryCache.get(context))
            // Coil spawns a new thread for every image load by default
            .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(8))
            .decoderCoroutineContext(Dispatchers.IO.limitedParallelism(2))
            .logger(
                object : CoilLogger {
                    override var minLevel = CoilLogLevel.Warn
                    override fun log(
                        tag: String,
                        level: CoilLogLevel,
                        message: String?,
                        throwable: Throwable?,
                    ) {
                        val exceptionName = throwable?.let { it::class.simpleName }
                        KermitLogger.processLog(
                            severity = when (level) {
                                CoilLogLevel.Verbose -> Severity.Verbose
                                CoilLogLevel.Debug -> Severity.Debug
                                CoilLogLevel.Info -> Severity.Info
                                CoilLogLevel.Warn, CoilLogLevel.Error -> Severity.Warn
                            },
                            tag = tag,
                            throwable = null,
                            message = exceptionName?.let { "Image load failed: $it" } ?: "Image loader reported a problem",
                        )
                    }
                },
            )
            .apply(imageLoaderBuilder)
            .build()
    }
}

internal object CoilDiskCache {
    private const val FOLDER_NAME = "image_cache"
    private var instance: DiskCache? = null


    fun get(): DiskCache {
        return instance ?: run {
            val safeCacheDir = FileKit.cacheDir.apply { createDirectories() }
            // Create the singleton disk cache instance.
            DiskCache.Builder()
                .directory(safeCacheDir.resolve(FOLDER_NAME).toKotlinxIoPath().toString().toPath())
                .build()
                .also { instance = it }
        }
    }
}

internal object CoilMemoryCache {
    private var instance: MemoryCache? = null

    fun get(context: PlatformContext): MemoryCache {
        return instance ?: run {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25)
                .build()
                .also { instance = it }
        }
    }
}
