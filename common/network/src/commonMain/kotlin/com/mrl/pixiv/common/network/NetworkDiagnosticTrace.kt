package com.mrl.pixiv.common.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

enum class NetworkDiagnosticStage { DNS, CONNECT, PROXY, TLS, HTTP, BODY }

enum class NetworkDiagnosticEventStatus { STARTED, SUCCEEDED, FAILED }

enum class NetworkDiagnosticRouteMode { DIRECT, HTTP, SOCKS, PROXY }

enum class NetworkDiagnosticResolver { SYSTEM, DOH, FALLBACK }

data class NetworkDiagnosticRoute(
    val mode: NetworkDiagnosticRouteMode,
    val peer: String? = null,
    val port: Int? = null,
)

data class NetworkDiagnosticEvent(
    val stage: NetworkDiagnosticStage,
    val status: NetworkDiagnosticEventStatus,
    val host: String? = null,
    val elapsedMillis: Long? = null,
    val route: NetworkDiagnosticRoute? = null,
    val addresses: List<String> = emptyList(),
    val resolvers: List<NetworkDiagnosticResolver> = emptyList(),
    val reusedConnection: Boolean? = null,
    val httpStatus: Int? = null,
    val proxyAuthenticationRequired: Boolean = false,
)

internal class NetworkDiagnosticTrace {
    private val observations = MutableStateFlow(emptyList<NetworkDiagnosticEvent>())
    private val pendingRequests = MutableStateFlow(0)
    val events: List<NetworkDiagnosticEvent> get() = observations.value

    fun record(event: NetworkDiagnosticEvent) {
        observations.update { it + event }
    }

    fun startRequest() {
        pendingRequests.update { it + 1 }
    }

    fun completeRequest() {
        pendingRequests.update { (it - 1).coerceAtLeast(0) }
    }

    suspend fun awaitPlatformObservation() {
        // URLSession metrics can arrive after a deliberately bounded response is closed.
        withTimeoutOrNull(300) { pendingRequests.first { it == 0 } }
    }
}

internal class DiagnosticProxyTargetUnavailable : Exception()

internal expect fun diagnosticHttpClient(
    settings: NetworkSettings,
    image: Boolean,
    trace: NetworkDiagnosticTrace,
): HttpClient

internal fun <T : HttpClientEngineConfig> diagnosticHttpClient(
    engine: HttpClientEngineFactory<T>,
    settings: NetworkSettings,
    image: Boolean,
    configure: T.() -> Unit,
): HttpClient = HttpClient(engine) {
    followRedirects = false
    engine(configure)
    install(HttpTimeout) {
        requestTimeoutMillis = 12000
        connectTimeoutMillis = 6000
        socketTimeoutMillis = 8000
    }
}.apply {
    if (image) applyPixivImageRequests { settings.imageHost }
}
