package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttpConfig
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import okhttp3.OkHttpClient

internal fun OkHttpClient.Builder.configureNetworkRouting(
    setting: BypassSetting,
    usePixivSni: Boolean,
): OkHttpClient.Builder = apply {
    // 代理由 OkHttp 选择，底层 TCP 不应再次套用 JVM 的系统 SOCKS 代理。
    socketFactory(DirectSocketFactory)
    when (setting) {
        BypassSetting.System -> {
            proxy(null)
            // Ktor 会复用基础 OkHttp 客户端，创建客户端时显式获取当前系统选择器。
            proxySelector(ProxySelector.getDefault() ?: DirectProxySelector)
        }
        BypassSetting.Direct -> proxy(Proxy.NO_PROXY)
        is BypassSetting.Proxy -> proxy(
            Proxy(
                Proxy.Type.valueOf(setting.proxyType.name),
                InetSocketAddress.createUnresolved(setting.host, setting.port),
            ),
        )
        is BypassSetting.SNI -> {
            if (usePixivSni) {
                bypassSNI(setting.url, setting.nonStrictSSL, setting.fallback, setting.dohTimeout)
            } else {
                // AI 与更新检查沿用直连，避免把 Pixiv 的 DNS、SNI 和证书策略用于其他服务。
                proxy(Proxy.NO_PROXY)
            }
        }
    }
}

private object DirectProxySelector : ProxySelector() {
    override fun select(uri: URI): List<Proxy> = listOf(Proxy.NO_PROXY)
    override fun connectFailed(uri: URI, address: SocketAddress, exception: IOException) = Unit
}

internal fun OkHttpConfig.configureProxyOrSNI(
    setting: BypassSetting = NetworkUtil.bypassSetting,
    usePixivSni: Boolean = true,
) {
    config { configureNetworkRouting(setting, usePixivSni) }
}

actual fun HttpClientConfig<*>.configureNetworkProxy(setting: BypassSetting) {
    engine { (this as OkHttpConfig).configureProxyOrSNI(setting, usePixivSni = false) }
}
