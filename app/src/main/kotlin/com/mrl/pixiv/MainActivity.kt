package com.mrl.pixiv

import android.app.ActivityManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import coil3.ImageLoader
import coil3.asImage
import coil3.compose.setSingletonImageLoaderFactory
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.allowRgb565
import com.mrl.pixiv.common.activity.BaseActivity
import com.mrl.pixiv.common.network.ImageClient
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.common.viewmodel.state
import com.mrl.pixiv.navigation.Navigation3MainGraph
import com.mrl.pixiv.splash.SplashViewModel
import com.mrl.pixiv.theme.PiPixivTheme
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.qualifier.named

class MainActivity : BaseActivity() {
    private val splashViewModel: SplashViewModel by viewModel()
    private val imageHttpClient: HttpClient by inject(named<ImageClient>())


    @Composable
    override fun BuildContent() {
        val darkTheme = isSystemInDarkTheme()
        LaunchedEffect(darkTheme) {
            // Draw edge-to-edge and set system bars color to transparent
            val lightStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
            val darkStyle = SystemBarStyle.dark(Color.TRANSPARENT)
            enableEdgeToEdge(
                statusBarStyle = if (darkTheme) darkStyle else lightStyle,
                navigationBarStyle = if (darkTheme) darkStyle else lightStyle,
            )
        }
        SetUpImageLoaderFactory()

        LaunchedEffect(Unit) {
            handleIntent(intent)
        }
        PiPixivTheme(
            darkTheme = darkTheme,
            colorScheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(
                        context
                    )
                }

                darkTheme -> darkColorScheme()
                else -> expressiveLightColorScheme()
            }
        ) {
            val state = splashViewModel.asState()
            state.startDestination?.let {
                Navigation3MainGraph(startDestination = it)
            }
        }
    }

    @Composable
    private fun SetUpImageLoaderFactory() {
        val errorImage =
            AppCompatResources.getDrawable(this, R.drawable.ic_error_outline_24)?.asImage()
        setSingletonImageLoaderFactory { context ->
            ImageLoader.Builder(context)
                .error(errorImage)
                .allowRgb565(getSystemService<ActivityManager>()!!.isLowRamDevice)
                .diskCache(CoilDiskCache.get(this))
                .memoryCache(CoilMemoryCache.get(this))
                .components {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        add(AnimatedImageDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                    add(KtorNetworkFetcherFactory(imageHttpClient))
                }
                // Coil spawns a new thread for every image load by default
                .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(8))
                .decoderCoroutineContext(Dispatchers.IO.limitedParallelism(2))
                .build()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                splashViewModel.state.isLoading
            }
        }
        super.onCreate(savedInstanceState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        splashViewModel.intent.update {
            intent
        }
    }
}