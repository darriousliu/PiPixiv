package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLException
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response

internal fun createOkHttpDiagnosticClient(
    settings: NetworkSettings,
    image: Boolean,
    trace: NetworkDiagnosticTrace,
    configure: OkHttpClient.Builder.() -> Unit = {},
): HttpClient = diagnosticHttpClient(OkHttp, settings, image) {
    val ech = settings.bypassSetting is BypassSetting.ECH
    val resolution = ThreadLocal<List<NetworkDiagnosticResolver>>()
    configureProxyOrECH(settings.bypassSetting, observeDns = resolution::set)
    config {
        cache(null)
        followRedirects(false)
        retryOnConnectionFailure(true)
        eventListenerFactory { DiagnosticEventListener(trace, ech, resolution) }
        configure()
    }
}

private class DiagnosticEventListener(
    private val trace: NetworkDiagnosticTrace,
    private val ech: Boolean,
    private val resolution: ThreadLocal<List<NetworkDiagnosticResolver>>,
) : EventListener() {
    private val dnsStarts = ConcurrentHashMap<String, TimeMark>()
    private val connectStarts = ConcurrentHashMap<NetworkDiagnosticRoute, TimeMark>()
    private val tlsStart = ThreadLocal<TimeMark>()
    private var establishedRoute: NetworkDiagnosticRoute? = null
    private var connectDuration: Long? = null
    private var requestStart: TimeMark? = null
    private var responseStatus: Int? = null
    private var bodyStart: TimeMark? = null

    override fun callStart(call: Call) = trace.startRequest()

    override fun dnsStart(call: Call, domainName: String) {
        resolution.remove()
        dnsStarts[domainName] = TimeSource.Monotonic.markNow()
        trace.record(
            NetworkDiagnosticEvent(NetworkDiagnosticStage.DNS, NetworkDiagnosticEventStatus.STARTED, domainName)
        )
    }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
        val resolvers = if (ech) resolution.get().orEmpty() else listOf(NetworkDiagnosticResolver.SYSTEM)
        resolution.remove()
        trace.record(
            NetworkDiagnosticEvent(
                stage = NetworkDiagnosticStage.DNS,
                status = NetworkDiagnosticEventStatus.SUCCEEDED,
                host = domainName,
                elapsedMillis = dnsStarts.remove(domainName).elapsedMillis(),
                addresses = inetAddressList.mapNotNull(InetAddress::getHostAddress),
                resolvers = resolvers,
            )
        )
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        val route = diagnosticRoute(inetSocketAddress, proxy)
        connectStarts[route] = TimeSource.Monotonic.markNow()
        record(call, NetworkDiagnosticStage.CONNECT, NetworkDiagnosticEventStatus.STARTED, route = route)
    }

    override fun secureConnectStart(call: Call) {
        tlsStart.set(TimeSource.Monotonic.markNow())
        record(call, NetworkDiagnosticStage.TLS, NetworkDiagnosticEventStatus.STARTED)
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        val duration = tlsStart.get().elapsedMillis()
        tlsStart.remove()
        val status = if (handshake != null) {
            NetworkDiagnosticEventStatus.SUCCEEDED
        } else {
            NetworkDiagnosticEventStatus.FAILED
        }
        record(call, NetworkDiagnosticStage.TLS, status, elapsedMillis = duration)
    }

    override fun connectEnd(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
    ) {
        val route = diagnosticRoute(inetSocketAddress, proxy)
        val duration = connectStarts.remove(route).elapsedMillis()
        if (protocol != null) {
            establishedRoute = route
            connectDuration = duration
        }
    }

    override fun connectionAcquired(call: Call, connection: Connection) {
        val route = connection.route().let { diagnosticRoute(it.socketAddress, it.proxy) }
        val wasEstablished = establishedRoute == route
        trace.record(
            NetworkDiagnosticEvent(
                stage = NetworkDiagnosticStage.CONNECT,
                status = NetworkDiagnosticEventStatus.SUCCEEDED,
                host = call.request().url.host,
                elapsedMillis = connectDuration.takeIf { wasEstablished },
                route = route,
                reusedConnection = !wasEstablished,
            )
        )
        establishedRoute = null
        connectDuration = null
    }

    override fun connectFailed(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
        ioe: IOException,
    ) {
        val route = diagnosticRoute(inetSocketAddress, proxy)
        val connect = connectStarts.remove(route)
        val tls = tlsStart.get()
        tlsStart.remove()
        if (call.isCanceled() || ioe.message.equals("canceled", ignoreCase = true) ||
            ioe.message.equals("socket closed", ignoreCase = true)
        ) return

        val proxyStatus = if (proxy.type() == Proxy.Type.HTTP) proxyConnectStatus(ioe) else null
        val socksRejected = proxy.type() == Proxy.Type.SOCKS && ioe.message.orEmpty().startsWith("SOCKS")
        val stage = when {
            proxyStatus != null || socksRejected -> NetworkDiagnosticStage.PROXY
            tls != null || ioe is SSLException -> NetworkDiagnosticStage.TLS
            else -> NetworkDiagnosticStage.CONNECT
        }
        // A CONNECT response or an attempted TLS handshake confirms the socket route.
        if (proxyStatus != null || tls != null) {
            record(call, NetworkDiagnosticStage.CONNECT, NetworkDiagnosticEventStatus.SUCCEEDED, route = route)
        }
        record(
            call,
            stage,
            NetworkDiagnosticEventStatus.FAILED,
            elapsedMillis = (tls ?: connect).elapsedMillis(),
            route = route,
            httpStatus = proxyStatus,
        )
    }

    override fun requestHeadersStart(call: Call) {
        requestStart = TimeSource.Monotonic.markNow()
        responseStatus = null
        bodyStart = null
        record(call, NetworkDiagnosticStage.HTTP, NetworkDiagnosticEventStatus.STARTED)
    }

    override fun responseHeadersEnd(call: Call, response: Response) {
        responseStatus = response.code
        val proxyAuth = response.code == 407
        val stage = if (proxyAuth) NetworkDiagnosticStage.PROXY else NetworkDiagnosticStage.HTTP
        val status = if (proxyAuth) NetworkDiagnosticEventStatus.FAILED else NetworkDiagnosticEventStatus.SUCCEEDED
        record(call, stage, status, elapsedMillis = requestStart.elapsedMillis(), httpStatus = response.code)
    }

    override fun responseBodyStart(call: Call) {
        bodyStart = TimeSource.Monotonic.markNow()
        record(call, NetworkDiagnosticStage.BODY, NetworkDiagnosticEventStatus.STARTED)
    }

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        record(
            call,
            NetworkDiagnosticStage.BODY,
            NetworkDiagnosticEventStatus.SUCCEEDED,
            elapsedMillis = bodyStart.elapsedMillis(),
        )
    }

    override fun requestFailed(call: Call, ioe: IOException) {
        if (!call.isCanceled()) {
            record(
                call,
                NetworkDiagnosticStage.HTTP,
                NetworkDiagnosticEventStatus.FAILED,
                elapsedMillis = requestStart.elapsedMillis(),
            )
        }
    }

    override fun responseFailed(call: Call, ioe: IOException) {
        if (call.isCanceled()) return
        val hasResponse = responseStatus != null
        record(
            call,
            if (hasResponse) NetworkDiagnosticStage.BODY else NetworkDiagnosticStage.HTTP,
            NetworkDiagnosticEventStatus.FAILED,
            elapsedMillis = (if (hasResponse) bodyStart else requestStart).elapsedMillis(),
        )
    }

    override fun callEnd(call: Call) = trace.completeRequest()

    override fun callFailed(call: Call, ioe: IOException) {
        if (!call.isCanceled() && ioe is UnknownHostException) {
            for ((host, started) in dnsStarts) {
                trace.record(
                    NetworkDiagnosticEvent(
                        NetworkDiagnosticStage.DNS,
                        NetworkDiagnosticEventStatus.FAILED,
                        host,
                        started.elapsedMillis(),
                    )
                )
            }
        }
        trace.completeRequest()
    }

    private fun record(
        call: Call,
        stage: NetworkDiagnosticStage,
        status: NetworkDiagnosticEventStatus,
        elapsedMillis: Long? = null,
        route: NetworkDiagnosticRoute? = null,
        httpStatus: Int? = null,
    ) = trace.record(
        NetworkDiagnosticEvent(stage, status, call.request().url.host, elapsedMillis, route, httpStatus = httpStatus)
    )
}

private fun diagnosticRoute(socketAddress: InetSocketAddress, proxy: Proxy): NetworkDiagnosticRoute {
    val mode = when (proxy.type()) {
        Proxy.Type.DIRECT -> NetworkDiagnosticRouteMode.DIRECT
        Proxy.Type.HTTP -> NetworkDiagnosticRouteMode.HTTP
        Proxy.Type.SOCKS -> NetworkDiagnosticRouteMode.SOCKS
    }
    // With SOCKS the route socket address is the origin sent to the proxy, not the connection peer.
    val peer = if (mode == NetworkDiagnosticRouteMode.SOCKS) proxy.address() as? InetSocketAddress else socketAddress
    return NetworkDiagnosticRoute(mode, peer?.address?.hostAddress ?: peer?.hostString, peer?.port)
}

private fun proxyConnectStatus(exception: IOException): Int? {
    // OkHttp surfaces rejected CONNECT responses as IOExceptions without an HTTP Response.
    val message = exception.message ?: return null
    if (message == "Failed to authenticate with proxy") return 407
    val prefix = "Unexpected response code for CONNECT: "
    return message.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)?.toIntOrNull()
}

private fun TimeMark?.elapsedMillis(): Long? = this?.elapsedNow()?.inWholeMilliseconds
