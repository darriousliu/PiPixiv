package com.mrl.pixiv

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.Image
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import com.mrl.pixiv.common.analytics.initKotzilla
import com.mrl.pixiv.common.util.isDebug
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.di.allModule
import com.mrl.pixiv.navigation.Navigation3MainGraph
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
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: ColorScheme = if (darkTheme) darkColorScheme() else expressiveLightColorScheme(),
    imageLoaderBuilder: ImageLoader.Builder.() -> Unit = {},
    splashViewModel: SplashViewModel = koinViewModel()
) {
    KoinApplication(
        application = {
            initKotzilla(isDebug)
            modules(allModule)
        }
    ) {
        SetUpImageLoaderFactory(imageLoaderBuilder)

        PiPixivTheme(
            darkTheme = darkTheme,
            colorScheme = colorScheme
        ) {
            val state = splashViewModel.asState()
            state.startDestination?.let {
                Navigation3MainGraph(
                    startDestination = it,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun SetUpImageLoaderFactory(imageLoaderBuilder: ImageLoader.Builder.() -> Unit) {
    // todo
//    val errorImage = getErrorImage()

    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
//            .error(errorImage)
            .diskCache(CoilDiskCache.get())
            .memoryCache(CoilMemoryCache.get(context))
            // Coil spawns a new thread for every image load by default
            .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(8))
            .decoderCoroutineContext(Dispatchers.IO.limitedParallelism(2))
            .apply(imageLoaderBuilder)
            .build()
    }
}

@Composable
expect fun getErrorImage(): Image

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