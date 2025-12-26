package com.mrl.pixiv

import androidx.compose.ui.window.ComposeUIViewController
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.mrl.pixiv.common.network.ImageClient
import io.ktor.client.HttpClient
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

fun MainViewController() = ComposeUIViewController {
    val imageHttpClient = koinInject<HttpClient>(named<ImageClient>())
    App(
        imageLoaderBuilder = {
            this.components {
                add(KtorNetworkFetcherFactory(imageHttpClient))
            }
        }
    )
}