package com.mrl.pixiv.common.network

import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.data.Constants.hostMap
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.network.NetworkUtil.imageHost
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust

internal actual val baseHttpClient: HttpClient
    get() = httpClient(Darwin) {
        configureRequest {
            setAllowsCellularAccess(true)
        }
//        configureHandleChallenge()
        configureProxy()
    }

internal actual val baseImageHttpClient: HttpClient
    get() = imageHttpClient(Darwin) {
        configureRequest {
            setAllowsCellularAccess(true)
        }
//        configureHandleChallenge()
        configureProxy()
    }

@OptIn(ExperimentalForeignApi::class)
private fun DarwinClientEngineConfig.configureHandleChallenge() {
    handleChallenge { _, _, challenge, completionHandler ->
        completionHandler as (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
        if (challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
            val host = challenge.protectionSpace.host
            if (host in hostMap.keys || host in hostMap.values || host == imageHost || host == "doh.dns.sb") {
                val credential =
                    NSURLCredential.credentialForTrust(challenge.protectionSpace.serverTrust!!)
                completionHandler(NSURLSessionAuthChallengeUseCredential, credential)
            } else {
                completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
            }
        } else {
            completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
        }
    }
}

internal fun DarwinClientEngineConfig.configureProxy(
    setting: UserPreference.BypassSetting = NetworkUtil.bypassSetting,
) {
    if (setting is UserPreference.BypassSetting.SNI) {
        Logger.w(tag = "HttpClient") { "iOS 不支持 SNI，使用直连" }
    }
    configureSession {
        connectionProxyDictionary = proxyConfiguration(setting)
    }
}

internal fun proxyConfiguration(setting: UserPreference.BypassSetting): Map<Any?, *>? {
    if (setting == UserPreference.BypassSetting.System) return null
    return buildMap<Any?, Any> {
        // 显式关闭系统代理与自动配置，确保直连和手动代理不会继承系统 PAC 配置。
        put("HTTPEnable", 0)
        put("HTTPSEnable", 0)
        put("SOCKSEnable", 0)
        put("ProxyAutoConfigEnable", 0)
        put("ProxyAutoDiscoveryEnable", 0)
        if (setting is UserPreference.BypassSetting.Proxy) {
            when (setting.proxyType) {
                UserPreference.BypassSetting.Proxy.ProxyType.HTTP -> {
                    put("HTTPEnable", 1)
                    put("HTTPProxy", setting.host)
                    put("HTTPPort", setting.port)
                    put("HTTPSEnable", 1)
                    put("HTTPSProxy", setting.host)
                    put("HTTPSPort", setting.port)
                }
                UserPreference.BypassSetting.Proxy.ProxyType.SOCKS -> {
                    put("SOCKSEnable", 1)
                    put("SOCKSProxy", setting.host)
                    put("SOCKSPort", setting.port)
                }
            }
        }
    }
}

actual fun HttpClientConfig<*>.configureNetworkProxy(setting: UserPreference.BypassSetting) {
    engine { (this as DarwinClientEngineConfig).configureProxy(setting) }
}

actual val httpEngineFactory: HttpClientEngineFactory<*> = Darwin
