package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.setting.UserPreference
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import java.net.InetSocketAddress
import java.net.Proxy

internal actual val baseHttpClient: HttpClient
    get() = httpClient(OkHttp) {
        config {
            followRedirects(true)
            retryOnConnectionFailure(true)
            hostnameVerifier(hostnameVerifier)
        }
        configureProxyOrSNI()
    }
internal actual val baseImageHttpClient: HttpClient
    get() = imageHttpClient(OkHttp) {
        config {
            followRedirects(true)
            retryOnConnectionFailure(true)
            hostnameVerifier(hostnameVerifier)
        }
        configureProxyOrSNI()
    }

private fun OkHttpConfig.configureProxyOrSNI() {
    when (val bypassSetting = NetworkUtil.bypassSetting) {
        is UserPreference.BypassSetting.None -> {}
        is UserPreference.BypassSetting.Proxy -> {
            proxy = Proxy(
                Proxy.Type.valueOf(bypassSetting.proxyType.name),
                InetSocketAddress.createUnresolved(
                    bypassSetting.host,
                    bypassSetting.port
                )
            )
        }

        is UserPreference.BypassSetting.SNI -> {
            config {
                bypassSNI(
                    bypassSetting.url,
                    bypassSetting.nonStrictSSL,
                    bypassSetting.fallback,
                    bypassSetting.dohTimeout
                )
            }
        }
    }
}