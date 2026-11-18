package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig

internal actual fun baseHttpClient(setting: BypassSetting): HttpClient = httpClient(Darwin) {
    configureRequest { setAllowsCellularAccess(true) }
    configureProxy(setting)
}

internal actual fun baseImageHttpClient(setting: BypassSetting): HttpClient = imageHttpClient(Darwin) {
    configureRequest { setAllowsCellularAccess(true) }
    configureProxy(setting)
}

internal fun DarwinClientEngineConfig.configureProxy(setting: BypassSetting) {
    configureSession { configureNetworkProxy(setting) }
}

actual fun HttpClientConfig<*>.configureNetworkProxy(setting: BypassSetting) {
    engine { (this as DarwinClientEngineConfig).configureProxy(setting) }
}

actual fun HttpClientConfig<*>.configurePixivNetworkProxy(setting: BypassSetting) =
    configureNetworkProxy(setting)

actual val httpEngineFactory: HttpClientEngineFactory<*> = Darwin
