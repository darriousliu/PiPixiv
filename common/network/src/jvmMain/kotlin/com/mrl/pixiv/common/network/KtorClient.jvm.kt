package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun baseHttpClient(
    setting: BypassSetting,
    allowRedirects: Boolean,
): HttpClient = httpClient(OkHttp) {
    config {
        followRedirects(allowRedirects)
        retryOnConnectionFailure(true)
    }
    configureProxyOrECH(setting)
}

internal actual fun baseImageHttpClient(setting: BypassSetting): HttpClient = imageHttpClient(OkHttp) {
    config {
        followRedirects(true)
        retryOnConnectionFailure(true)
    }
    configureProxyOrECH(setting)
}

actual val httpEngineFactory: HttpClientEngineFactory<*> = OkHttp

internal actual fun diagnosticHttpClient(
    settings: NetworkSettings,
    image: Boolean,
    trace: NetworkDiagnosticTrace,
): HttpClient = createOkHttpDiagnosticClient(settings, image, trace)
