package com.mrl.pixiv.common.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

internal actual val baseHttpClient: HttpClient
    get() = httpClient(OkHttp) {
        config {
            followRedirects(true)
            retryOnConnectionFailure(true)
        }
        configureProxyOrSNI()
    }
internal actual val baseImageHttpClient: HttpClient
    get() = imageHttpClient(OkHttp) {
        config {
            followRedirects(true)
            retryOnConnectionFailure(true)
        }
        configureProxyOrSNI()
    }

actual val httpEngineFactory: HttpClientEngineFactory<*> = OkHttp
