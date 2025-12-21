package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.Constants.hostMap
import com.mrl.pixiv.common.network.NetworkUtil.imageHost
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust

internal actual val baseHttpClient: HttpClient
    get() = httpClient(Darwin) {
        configureRequest {
            setAllowsCellularAccess(true)
        }
        configureHandleChallenge()
    }

internal actual val baseImageHttpClient: HttpClient
    get() = httpClient(Darwin) {
        configureRequest {
            setAllowsCellularAccess(true)
        }
        configureHandleChallenge()
    }

private fun DarwinClientEngineConfig.configureHandleChallenge() {
    handleChallenge { _, _, challenge, completionHandler ->
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

