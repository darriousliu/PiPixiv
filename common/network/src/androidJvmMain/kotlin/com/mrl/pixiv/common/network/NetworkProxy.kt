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
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient

internal fun OkHttpClient.Builder.configureNetworkRouting(
    setting: BypassSetting,
    usePixivEch: Boolean,
    dohClient: OkHttpClient? = null,
    observeDns: ((List<NetworkDiagnosticResolver>) -> Unit)? = null,
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
        is BypassSetting.ECH -> {
            if (usePixivEch) {
                bypassECH(setting.url, dohClient ?: OkHttpClient(), observeDns)
            } else {
                // AI 与更新检查沿用直连，避免把 Pixiv 的 DNS、ECH 和证书策略用于其他服务。
                proxy(Proxy.NO_PROXY)
            }
        }
    }
}

private object DirectProxySelector : ProxySelector() {
    override fun select(uri: URI): List<Proxy> = listOf(Proxy.NO_PROXY)
    override fun connectFailed(uri: URI, address: SocketAddress, exception: IOException) = Unit
}

internal fun OkHttpConfig.configureProxyOrECH(
    setting: BypassSetting,
    usePixivEch: Boolean = true,
    observeDns: ((List<NetworkDiagnosticResolver>) -> Unit)? = null,
) {
    val pool = ConnectionPool()
    val dispatcher = Dispatcher()
    val dohClient = if (setting is BypassSetting.ECH && usePixivEch) {
        OkHttpClient.Builder().connectionPool(pool).dispatcher(dispatcher).build()
    } else {
        null
    }
    config {
        connectionPool(pool)
        dispatcher(dispatcher)
        configureNetworkRouting(setting, usePixivEch, dohClient, observeDns)
    }
}

actual fun HttpClientConfig<*>.configureNetworkProxy(setting: BypassSetting) {
    engine { (this as OkHttpConfig).configureProxyOrECH(setting, usePixivEch = false) }
}

actual fun HttpClientConfig<*>.configurePixivNetworkProxy(setting: BypassSetting) {
    engine { (this as OkHttpConfig).configureProxyOrECH(setting) }
}
