package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.Constants.IMAGE_HOST
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.KtorNSURLSessionDelegate
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import platform.CFNetwork.kCFErrorHTTPProxyConnectionFailure
import platform.CFNetwork.kCFErrorHTTPSProxyConnectionFailure
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURL
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLErrorCancelled
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorClientCertificateRejected
import platform.Foundation.NSURLErrorClientCertificateRequired
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorSecureConnectionFailed
import platform.Foundation.NSURLErrorServerCertificateHasBadDate
import platform.Foundation.NSURLErrorServerCertificateHasUnknownRoot
import platform.Foundation.NSURLErrorServerCertificateNotYetValid
import platform.Foundation.NSURLErrorServerCertificateUntrusted
import platform.Foundation.NSURLErrorTimedOut
import platform.Foundation.NSURLErrorUserAuthenticationRequired
import platform.Foundation.NSURLErrorUserCancelledAuthentication
import platform.Foundation.NSURLProtectionSpaceHTTPProxy
import platform.Foundation.NSURLProtectionSpaceHTTPSProxy
import platform.Foundation.NSURLProtectionSpaceSOCKSProxy
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLSessionTaskMetrics
import platform.Foundation.NSURLSessionTaskMetricsDomainResolutionProtocol.NSURLSessionTaskMetricsDomainResolutionProtocolHTTPS
import platform.Foundation.NSURLSessionTaskMetricsDomainResolutionProtocol.NSURLSessionTaskMetricsDomainResolutionProtocolTCP
import platform.Foundation.NSURLSessionTaskMetricsDomainResolutionProtocol.NSURLSessionTaskMetricsDomainResolutionProtocolTLS
import platform.Foundation.NSURLSessionTaskMetricsDomainResolutionProtocol.NSURLSessionTaskMetricsDomainResolutionProtocolUDP
import platform.Foundation.NSURLSessionTaskMetricsResourceFetchType.NSURLSessionTaskMetricsResourceFetchTypeNetworkLoad
import platform.Foundation.NSURLSessionTaskTransactionMetrics
import platform.Foundation.timeIntervalSinceDate
import platform.darwin.NSObject
import platform.darwin.inet_aton
import platform.darwin.inet_pton
import platform.posix.AF_INET6
import platform.posix.in6_addr
import platform.posix.in_addr

internal actual fun diagnosticHttpClient(
    settings: NetworkSettings,
    image: Boolean,
    trace: NetworkDiagnosticTrace,
): HttpClient {
    val bypassSetting = settings.bypassSetting
    if (image && bypassSetting is BypassSetting.Proxy && settings.imageHost.isLoopbackImageSource()) {
        // URLSession bypasses even a failover-disabled proxy for loopback targets.
        throw DiagnosticProxyTargetUnavailable()
    }
    return diagnosticHttpClient(Darwin, settings, image) {
        val configuration = NSURLSessionConfiguration.ephemeralSessionConfiguration().apply {
            configureNetworkProxy(bypassSetting)
            setURLCache(null)
            setRequestCachePolicy(NSURLRequestReloadIgnoringLocalCacheData)
            setHTTPCookieStorage(null)
            setURLCredentialStorage(null)
        }
        val ktorDelegate = KtorNSURLSessionDelegate()
        val session = NSURLSession.sessionWithConfiguration(
            configuration,
            DarwinDiagnosticDelegate(ktorDelegate, trace),
            delegateQueue = null,
        )
        usePreconfiguredSession(session, ktorDelegate)
        configureRequest {
            setAllowsCellularAccess(true)
            trace.startRequest()
        }
    }
}

private fun String.isLoopbackImageSource(): Boolean = isLoopbackTarget() ||
    NSURL.URLWithString(resolveImageUrl("https://$IMAGE_HOST/", this))?.host?.isLoopbackTarget() == true

@OptIn(ExperimentalForeignApi::class)
private fun String.isLoopbackTarget(): Boolean {
    val host = trim().removeSuffix(".").removeSurrounding("[", "]").substringBefore('%').lowercase()
    if (host == "localhost" || host.endsWith(".localhost")) return true

    return memScoped {
        val ipv4 = alloc<in_addr>()
        if (inet_aton(host, ipv4.ptr) == 1) {
            val bytes = ipv4.ptr.reinterpret<UByteVar>()
            return@memScoped ipv4.s_addr == 0u || bytes[0].toInt() == 127
        }

        val ipv6 = alloc<in6_addr>()
        if (inet_pton(AF_INET6, host, ipv6.ptr) != 1) return@memScoped false
        val bytes = ipv6.ptr.reinterpret<UByteVar>()
        val isIpv6Local = (0..14).all { bytes[it] == 0.toUByte() } && bytes[15].toInt() in 0..1
        val embedsIpv4 = (0..9).all { bytes[it] == 0.toUByte() } &&
            bytes[10] == bytes[11] && bytes[10].toInt() in setOf(0, 255)
        val isEmbeddedIpv4Local = embedsIpv4 && (bytes[12] == 127.toUByte() ||
            (12..15).all { bytes[it] == 0.toUByte() })
        isIpv6Local || isEmbeddedIpv4Local
    }
}

private class DarwinDiagnosticDelegate(
    private val ktorDelegate: KtorNSURLSessionDelegate,
    private val trace: NetworkDiagnosticTrace,
) : NSObject(), NSURLSessionDataDelegateProtocol {
    // A session made with the default delegate queue serializes delegate callbacks.
    private val lastUnfinishedStages = mutableMapOf<ULong, NetworkDiagnosticStage>()
    private val proxyChallenges = mutableMapOf<ULong, ProxyChallenge>()

    override fun URLSession(session: NSURLSession, dataTask: NSURLSessionDataTask, didReceiveData: NSData) {
        ktorDelegate.URLSession(session, dataTask, didReceiveData)
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        willPerformHTTPRedirection: NSHTTPURLResponse,
        newRequest: NSURLRequest,
        completionHandler: (NSURLRequest?) -> Unit,
    ) {
        ktorDelegate.URLSession(session, task, willPerformHTTPRedirection, newRequest, completionHandler)
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didReceiveChallenge: NSURLAuthenticationChallenge,
        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
    ) {
        val protectionSpace = didReceiveChallenge.protectionSpace
        if (protectionSpace.isProxy()) {
            val route = NetworkDiagnosticRoute(
                mode = when (protectionSpace.proxyType) {
                    NSURLProtectionSpaceHTTPProxy, NSURLProtectionSpaceHTTPSProxy -> NetworkDiagnosticRouteMode.HTTP
                    NSURLProtectionSpaceSOCKSProxy -> NetworkDiagnosticRouteMode.SOCKS
                    else -> NetworkDiagnosticRouteMode.PROXY
                },
                peer = protectionSpace.host,
                port = protectionSpace.port.toInt().takeIf { it > 0 },
            )
            val status = (didReceiveChallenge.failureResponse as? NSHTTPURLResponse)?.statusCode?.toInt()
            proxyChallenges[task.taskIdentifier] = ProxyChallenge(route, status)
        }
        ktorDelegate.URLSession(session, task, didReceiveChallenge, completionHandler)
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didFinishCollectingMetrics: NSURLSessionTaskMetrics,
    ) {
        val transaction = didFinishCollectingMetrics.transactionMetrics
            .filterIsInstance<NSURLSessionTaskTransactionMetrics>()
        transaction.forEach { metrics ->
            val unfinished = recordMetrics(metrics)
            if (unfinished == null) {
                lastUnfinishedStages.remove(task.taskIdentifier)
            } else {
                lastUnfinishedStages[task.taskIdentifier] = unfinished
            }
        }
    }

    override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) {
        val proxyChallenge = proxyChallenges.remove(task.taskIdentifier)
        val unfinishedStage = lastUnfinishedStages.remove(task.taskIdentifier)
        if (didCompleteWithError != null) {
            val stage = didCompleteWithError.failureStage(unfinishedStage, proxyChallenge != null)
            if (stage != null) {
                val host = (task.currentRequest ?: task.originalRequest)?.URL?.host
                val proxyAuth = proxyChallenge.takeIf { stage == NetworkDiagnosticStage.PROXY }
                if (proxyAuth != null) {
                    trace.record(
                        NetworkDiagnosticEvent(
                            NetworkDiagnosticStage.CONNECT,
                            NetworkDiagnosticEventStatus.SUCCEEDED,
                            host,
                            route = proxyAuth.route,
                        )
                    )
                }
                trace.record(
                    NetworkDiagnosticEvent(
                        stage,
                        NetworkDiagnosticEventStatus.FAILED,
                        host,
                        httpStatus = proxyAuth?.status,
                        proxyAuthenticationRequired = proxyAuth != null,
                    )
                )
            }
        }
        // Ktor may resume the caller from this callback. Publish native evidence first.
        trace.completeRequest()
        ktorDelegate.URLSession(session, task, didCompleteWithError)
    }

    private fun recordMetrics(metrics: NSURLSessionTaskTransactionMetrics): NetworkDiagnosticStage? {
        val host = metrics.request.URL?.host
        val connected = metrics.resourceFetchType == NSURLSessionTaskMetricsResourceFetchTypeNetworkLoad &&
            (metrics.connectEndDate != null || metrics.reusedConnection || metrics.responseStartDate != null)
        val route = if (metrics.proxyConnection || connected) {
            NetworkDiagnosticRoute(
                mode = if (metrics.proxyConnection) NetworkDiagnosticRouteMode.PROXY else NetworkDiagnosticRouteMode.DIRECT,
                peer = metrics.remoteAddress,
                port = metrics.remotePort?.intValue,
            )
        } else {
            null
        }

        val dnsPending = recordTiming(
            NetworkDiagnosticStage.DNS,
            host.takeUnless { metrics.proxyConnection },
            metrics.domainLookupStartDate,
            metrics.domainLookupEndDate,
            resolvers = when (metrics.domainResolutionProtocol) {
                NSURLSessionTaskMetricsDomainResolutionProtocolHTTPS -> listOf(NetworkDiagnosticResolver.DOH)
                NSURLSessionTaskMetricsDomainResolutionProtocolTCP,
                NSURLSessionTaskMetricsDomainResolutionProtocolTLS,
                NSURLSessionTaskMetricsDomainResolutionProtocolUDP -> listOf(NetworkDiagnosticResolver.SYSTEM)
                else -> emptyList()
            },
        )
        val connectPending = recordTiming(
            stage = NetworkDiagnosticStage.CONNECT,
            host = host,
            start = metrics.connectStartDate,
            end = metrics.connectEndDate,
            route = route.takeIf { connected || metrics.remoteAddress != null },
            reusedConnection = metrics.reusedConnection.takeIf { route != null },
        )
        val tlsPending = recordTiming(NetworkDiagnosticStage.TLS, host, metrics.secureConnectionStartDate, metrics.secureConnectionEndDate)
        val status = (metrics.response as? NSHTTPURLResponse)?.statusCode?.toInt()
        if (status != null) {
            trace.record(
                NetworkDiagnosticEvent(
                    stage = if (status == 407) NetworkDiagnosticStage.PROXY else NetworkDiagnosticStage.HTTP,
                    status = if (status == 407) NetworkDiagnosticEventStatus.FAILED else NetworkDiagnosticEventStatus.SUCCEEDED,
                    host = host,
                    elapsedMillis = durationMillis(metrics.requestStartDate, metrics.responseStartDate),
                    httpStatus = status,
                )
            )
        }
        val bodyPending = recordTiming(NetworkDiagnosticStage.BODY, host, metrics.responseStartDate, metrics.responseEndDate)
        return when {
            bodyPending -> NetworkDiagnosticStage.BODY
            metrics.requestStartDate != null && metrics.responseStartDate == null -> NetworkDiagnosticStage.HTTP
            tlsPending -> NetworkDiagnosticStage.TLS
            connectPending -> NetworkDiagnosticStage.CONNECT
            dnsPending -> NetworkDiagnosticStage.DNS
            else -> null
        }
    }

    private fun recordTiming(
        stage: NetworkDiagnosticStage,
        host: String?,
        start: NSDate?,
        end: NSDate?,
        route: NetworkDiagnosticRoute? = null,
        reusedConnection: Boolean? = null,
        resolvers: List<NetworkDiagnosticResolver> = emptyList(),
    ): Boolean {
        if (start == null && route == null) return false
        val pending = end == null && reusedConnection != true && start != null
        trace.record(
            NetworkDiagnosticEvent(
                stage = stage,
                status = if (pending) NetworkDiagnosticEventStatus.STARTED else NetworkDiagnosticEventStatus.SUCCEEDED,
                host = host,
                elapsedMillis = durationMillis(start, end),
                route = route,
                resolvers = resolvers,
                reusedConnection = reusedConnection,
            )
        )
        return pending
    }

    private data class ProxyChallenge(val route: NetworkDiagnosticRoute, val status: Int?)
}

private fun durationMillis(start: NSDate?, end: NSDate?): Long? =
    if (start == null || end == null) null else (end.timeIntervalSinceDate(start) * 1000).toLong().coerceAtLeast(0)

private fun NSError.failureStage(
    unfinishedStage: NetworkDiagnosticStage?,
    receivedProxyChallenge: Boolean,
): NetworkDiagnosticStage? = when (domain) {
    "kCFErrorDomainCFNetwork" -> when (code.toInt()) {
        kCFErrorHTTPProxyConnectionFailure, kCFErrorHTTPSProxyConnectionFailure -> NetworkDiagnosticStage.PROXY
        else -> null
    }
    NSURLErrorDomain -> when (code) {
        NSURLErrorCancelled -> null
        NSURLErrorUserAuthenticationRequired, NSURLErrorUserCancelledAuthentication ->
            NetworkDiagnosticStage.PROXY.takeIf { receivedProxyChallenge }
        NSURLErrorCannotFindHost, NSURLErrorDNSLookupFailed -> NetworkDiagnosticStage.DNS
        NSURLErrorCannotConnectToHost -> NetworkDiagnosticStage.CONNECT
        NSURLErrorSecureConnectionFailed,
        NSURLErrorServerCertificateHasBadDate,
        NSURLErrorServerCertificateUntrusted,
        NSURLErrorServerCertificateHasUnknownRoot,
        NSURLErrorServerCertificateNotYetValid,
        NSURLErrorClientCertificateRejected,
        NSURLErrorClientCertificateRequired -> NetworkDiagnosticStage.TLS
        NSURLErrorTimedOut, NSURLErrorNetworkConnectionLost -> unfinishedStage
        else -> null
    }
    else -> null
}
