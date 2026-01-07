package com.mrl.pixiv.common.network

import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.data.Constants.hostMap
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.network.NetworkUtil.imageHost
import io.ktor.client.HttpClient
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
    get() = httpClient(Darwin) {
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

private fun DarwinClientEngineConfig.configureProxy() {
    when (val bypassSetting = NetworkUtil.bypassSetting) {
        is UserPreference.BypassSetting.None -> {}
        is UserPreference.BypassSetting.Proxy -> {
            configureSession {
                connectionProxyDictionary = buildMap {
                    if (bypassSetting.proxyType == UserPreference.BypassSetting.Proxy.ProxyType.HTTP) {
                        put("HTTPEnable", true)
                        put("HTTPProxy", bypassSetting.host)
                        put("HTTPPort", bypassSetting.port)

                        put("HTTPSEnable", true)
                        put("HTTPSProxy", bypassSetting.host)
                        put("HTTPSPort", bypassSetting.port)
                    }
                    if (bypassSetting.proxyType == UserPreference.BypassSetting.Proxy.ProxyType.SOCKS) {
                        put("SOCKSEnable", true)
                        put("SOCKSProxy", bypassSetting.host)
                        put("SOCKSPort", bypassSetting.port)
                    }
                }
            }
//            proxy = when (bypassSetting.proxyType) {
//                UserPreference.BypassSetting.Proxy.ProxyType.HTTP -> ProxyBuilder.http(
//                    URLBuilder(
//                        protocol = URLProtocol.HTTP,
//                        host = bypassSetting.host,
//                        port = bypassSetting.port
//                    ).build()
//                )
//
//                UserPreference.BypassSetting.Proxy.ProxyType.SOCKS -> ProxyBuilder.socks(
//                    host = bypassSetting.host,
//                    port = bypassSetting.port
//                )
//            }
        }

        is UserPreference.BypassSetting.SNI -> {
            Logger.w("HttpClient") { "SNI is not supported on iOS" }
        }
    }
}

