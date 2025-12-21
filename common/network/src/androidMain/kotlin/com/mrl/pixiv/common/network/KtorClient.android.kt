package com.mrl.pixiv.common.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

internal actual val baseHttpClient: HttpClient
    get() = httpClient(OkHttp) {
        config {
            retryOnConnectionFailure(true)
            hostnameVerifier(hostnameVerifier)
        }
    }
internal actual val baseImageHttpClient: HttpClient
    get() = imageHttpClient(OkHttp) {
        config {
            retryOnConnectionFailure(true)
            hostnameVerifier(hostnameVerifier)
        }
    }