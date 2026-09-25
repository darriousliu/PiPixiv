package com.mrl.pixiv.common.network

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
    configureSession {
        configureNetworkProxy(setting)
    }
}

actual fun HttpClientConfig<*>.configureNetworkProxy(setting: UserPreference.BypassSetting) {
    engine { (this as DarwinClientEngineConfig).configureProxy(setting) }
}

actual val httpEngineFactory: HttpClientEngineFactory<*> = Darwin
