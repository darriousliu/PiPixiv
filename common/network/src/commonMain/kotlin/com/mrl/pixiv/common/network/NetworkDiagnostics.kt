package com.mrl.pixiv.common.network

import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentLength
import io.ktor.http.takeFrom
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.koin.core.annotation.Single
import kotlin.time.TimeSource

enum class NetworkDiagnosticTarget { APP_API, WEB, LOGIN, IMAGE }

enum class NetworkDiagnosticStatus { SUCCESS, REQUIRES_LOGIN, FAILED }

enum class NetworkDiagnosticFailure {
    TIMEOUT, TRANSPORT, PROXY_AUTH, PROXY_UNSUPPORTED_TARGET, HTTP_STATUS, RATE_LIMITED, UNEXPECTED_RESPONSE,
    REDIRECT, SAMPLE_NOT_FOUND, SIZE_LIMIT, INCOMPLETE_BODY,
}

data class NetworkDiagnosticResult(
    val target: NetworkDiagnosticTarget,
    val host: String,
    val status: NetworkDiagnosticStatus,
    val elapsedMillis: Long,
    val httpStatus: Int? = null,
    val downloadedBytes: Long? = null,
    val failure: NetworkDiagnosticFailure? = null,
    val failureStage: NetworkDiagnosticStage? = null,
    val events: List<NetworkDiagnosticEvent> = emptyList(),
)

@Single
fun networkDiagnostics() = NetworkDiagnostics()

internal data class NetworkDiagnosticEndpoints(
    val appApi: String,
    val web: String,
    val login: String,
    val image: String,
    val fallbackImage: String? = null,
) {
    val allowedWebOrigins = setOf(Url(web).origin(), Url(login).origin())

    companion object {
        val Default = NetworkDiagnosticEndpoints(
            appApi = "https://app-api.pixiv.net/v1/illust/ranking?mode=day&filter=for_android",
            web = "https://www.pixiv.net/",
            login = "https://accounts.pixiv.net/login",
            // Fixed public CDN samples keep this probe independent of API and web availability.
            image = "https://i.pximg.net/c/540x540_70/img-master/img/2019/03/30/16/33/50/73949833_p0_master1200.jpg",
            fallbackImage = "https://i.pximg.net/user-profile/img/2017/04/27/10/00/38/12474975_a0a699ea19f387df0f98bc5a9b7d26d3_170.png",
        )
    }
}

class NetworkDiagnostics internal constructor(
    private val endpoints: NetworkDiagnosticEndpoints,
    private val clientFactory: (NetworkSettings, Boolean, NetworkDiagnosticTrace) -> HttpClient,
) {
    constructor() : this(NetworkDiagnosticEndpoints.Default, ::diagnosticHttpClient)

    suspend fun run(settings: NetworkSettings, onUpdate: (NetworkDiagnosticResult) -> Unit) {
        onUpdate(probe(settings, NetworkDiagnosticTarget.APP_API, endpoints.appApi))
        onUpdate(probe(settings, NetworkDiagnosticTarget.WEB, endpoints.web))
        onUpdate(probe(settings, NetworkDiagnosticTarget.LOGIN, endpoints.login))
        onUpdate(probe(settings, NetworkDiagnosticTarget.IMAGE, endpoints.image))
    }

    private suspend fun probe(
        settings: NetworkSettings,
        target: NetworkDiagnosticTarget,
        url: String,
    ): NetworkDiagnosticResult {
        val started = TimeSource.Monotonic.markNow()
        val trace = NetworkDiagnosticTrace()
        val image = target == NetworkDiagnosticTarget.IMAGE
        val requestedHost = Url(url).host
        val state = ProbeProgress(if (image) settings.imageHost.ifEmpty { requestedHost } else requestedHost)
        var failureStage: NetworkDiagnosticStage? = null
        val status = try {
            withTimeoutOrNull(PROBE_TIMEOUT_MS) {
                clientFactory(settings, image, trace).use { client ->
                    when (target) {
                        NetworkDiagnosticTarget.APP_API -> probeApi(client, url, state)
                        NetworkDiagnosticTarget.WEB, NetworkDiagnosticTarget.LOGIN ->
                            probeWeb(client, url, state, trace)
                        NetworkDiagnosticTarget.IMAGE -> probeImage(client, url, state, trace)
                    }
                }
            } ?: throw ProbeError(NetworkDiagnosticFailure.TIMEOUT, state.stage)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            failureStage = when (error) {
                is DiagnosticProxyTargetUnavailable -> NetworkDiagnosticStage.PROXY
                is ProbeError -> error.stage ?: state.stage
                else -> state.stage
            }
            state.failure = when (error) {
                is ProbeError -> error.failure
                is DiagnosticProxyTargetUnavailable -> NetworkDiagnosticFailure.PROXY_UNSUPPORTED_TARGET
                is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
                    NetworkDiagnosticFailure.TIMEOUT
                else -> NetworkDiagnosticFailure.TRANSPORT
            }
            NetworkDiagnosticStatus.FAILED
        }
        val elapsedMillis = started.elapsedNow().inWholeMilliseconds
        trace.awaitPlatformObservation()
        val events = trace.events
        if (state.failure != null && failureStage == null) {
            val currentEvents = events.drop(state.firstEvent)
            val last = currentEvents.lastOrNull { it.status == NetworkDiagnosticEventStatus.FAILED }
                ?: currentEvents.lastOrNull { it.status == NetworkDiagnosticEventStatus.STARTED }
            failureStage = last?.stage
            if (last?.stage == NetworkDiagnosticStage.PROXY &&
                (last.httpStatus == 407 || last.proxyAuthenticationRequired)
            ) {
                state.httpStatus = last.httpStatus
                state.failure = NetworkDiagnosticFailure.PROXY_AUTH
            }
        }
        return NetworkDiagnosticResult(
            target = target,
            host = state.host,
            status = status,
            elapsedMillis = elapsedMillis,
            httpStatus = state.httpStatus,
            downloadedBytes = state.downloadedBytes,
            failure = state.failure,
            failureStage = failureStage,
            events = events,
        )
    }

    private suspend fun probeApi(client: HttpClient, url: String, state: ProbeProgress) =
        client.prepareGet(url) {
            NetworkUtil.addAppHeaders(this)
            header(HttpHeaders.Accept, "application/json")
        }.execute { response ->
            state.headers(response)
            val status = response.status.value
            if (status !in setOf(200, 400, 401, 403)) checkHttpFailure(status)
            if (response.mediaType() != "application/json") {
                if (status != 200 && status != 401) checkHttpFailure(status)
                throw ProbeError(NetworkDiagnosticFailure.UNEXPECTED_RESPONSE, NetworkDiagnosticStage.HTTP)
            }
            val body = response.readPrefix().decodeToString()
            val json = runCatching { Json.parseToJsonElement(body) }.getOrNull() as? JsonObject
            val error = json?.get("error") as? JsonObject
            val message = (error?.get("message") as? JsonPrimitive)?.content.orEmpty()
            when {
                status == 200 && json?.get("illusts") is JsonArray -> NetworkDiagnosticStatus.SUCCESS
                status != 200 && isAuthenticationError(message) -> NetworkDiagnosticStatus.REQUIRES_LOGIN
                status != 200 && status != 401 ->
                    throw ProbeError(NetworkDiagnosticFailure.HTTP_STATUS, NetworkDiagnosticStage.HTTP)
                else -> throw ProbeError(NetworkDiagnosticFailure.UNEXPECTED_RESPONSE, NetworkDiagnosticStage.BODY)
            }
        }

    private suspend fun probeWeb(
        client: HttpClient,
        url: String,
        state: ProbeProgress,
        trace: NetworkDiagnosticTrace,
    ): NetworkDiagnosticStatus {
        var current = Url(url)
        repeat(MAX_WEB_REDIRECTS + 1) { hop ->
            state.nextRequest(current.host, trace.events.size)
            val next = client.prepareGet(current) {
                header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            }.execute { response ->
                state.headers(response)
                val status = response.status.value
                if (status in REDIRECT_STATUSES) {
                    val location = response.headers[HttpHeaders.Location]
                    val destination = location?.let { redirectFrom(current, it) }
                    if (hop == MAX_WEB_REDIRECTS || destination == null ||
                        destination.origin() !in endpoints.allowedWebOrigins
                    ) {
                        throw ProbeError(NetworkDiagnosticFailure.REDIRECT, NetworkDiagnosticStage.HTTP)
                    }
                    destination
                } else {
                    checkHttpFailure(status)
                    if (response.mediaType() !in setOf("text/html", "application/xhtml+xml")) {
                        throw ProbeError(NetworkDiagnosticFailure.UNEXPECTED_RESPONSE, NetworkDiagnosticStage.HTTP)
                    }
                    val html = response.readPrefix().decodeToString().lowercase()
                    if (!isPixivPage(html)) {
                        throw ProbeError(NetworkDiagnosticFailure.UNEXPECTED_RESPONSE, NetworkDiagnosticStage.BODY)
                    }
                    null
                }
            }
            current = next ?: return NetworkDiagnosticStatus.SUCCESS
            trace.awaitPlatformObservation()
        }
        error("Redirect limit must be applied before the next request")
    }

    private suspend fun probeImage(
        client: HttpClient,
        url: String,
        state: ProbeProgress,
        trace: NetworkDiagnosticTrace,
    ): NetworkDiagnosticStatus {
        try {
            readImage(client, url, state)
        } catch (error: ProbeError) {
            val fallback = endpoints.fallbackImage
            if (error.failure != NetworkDiagnosticFailure.SAMPLE_NOT_FOUND || fallback == null) throw error
            trace.awaitPlatformObservation()
            state.nextRequest(state.host, trace.events.size)
            readImage(client, fallback, state)
        }
        return NetworkDiagnosticStatus.SUCCESS
    }

    private suspend fun readImage(client: HttpClient, url: String, state: ProbeProgress) {
        client.prepareGet(url) {
            header(HttpHeaders.Accept, "image/*")
            header(HttpHeaders.AcceptEncoding, "identity")
        }.execute { response ->
            state.headers(response)
            val status = response.status.value
            if (status == 404 || status == 410) {
                throw ProbeError(NetworkDiagnosticFailure.SAMPLE_NOT_FOUND, NetworkDiagnosticStage.HTTP)
            }
            checkHttpFailure(status)
            val type = response.mediaType()
            if (status != 200 || type !in IMAGE_TYPES) {
                throw ProbeError(NetworkDiagnosticFailure.UNEXPECTED_RESPONSE, NetworkDiagnosticStage.HTTP)
            }
            val expected = response.contentLength()
            if (expected != null && expected > MAX_IMAGE_BYTES) {
                throw ProbeError(NetworkDiagnosticFailure.SIZE_LIMIT, NetworkDiagnosticStage.BODY)
            }

            val channel = response.bodyAsChannel()
            val buffer = ByteArray(8192)
            val prefix = ByteArray(16)
            var prefixSize = 0
            var received = 0L
            state.downloadedBytes = 0L
            try {
                while (received <= MAX_IMAGE_BYTES) {
                    val limit = minOf(buffer.size.toLong(), MAX_IMAGE_BYTES - received + 1).toInt()
                    val count = channel.readAvailable(buffer, length = limit)
                    if (count == -1) break
                    val toCopy = minOf(count, prefix.size - prefixSize)
                    buffer.copyInto(prefix, prefixSize, endIndex = toCopy)
                    prefixSize += toCopy
                    received += count
                    state.downloadedBytes = received
                }
                channel.closedCause?.let { throw it }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (error is SocketTimeoutException) throw error
                throw ProbeError(NetworkDiagnosticFailure.INCOMPLETE_BODY, NetworkDiagnosticStage.BODY)
            }
            if (received > MAX_IMAGE_BYTES) {
                throw ProbeError(NetworkDiagnosticFailure.SIZE_LIMIT, NetworkDiagnosticStage.BODY)
            }
            if (expected != null && received != expected) {
                throw ProbeError(NetworkDiagnosticFailure.INCOMPLETE_BODY, NetworkDiagnosticStage.BODY)
            }
            if (!hasImageSignature(type, prefix.copyOf(prefixSize))) {
                throw ProbeError(NetworkDiagnosticFailure.UNEXPECTED_RESPONSE, NetworkDiagnosticStage.BODY)
            }
        }
    }
}

private class ProbeProgress(var host: String) {
    var firstEvent = 0
        private set
    var stage: NetworkDiagnosticStage? = null
    var httpStatus: Int? = null
    var downloadedBytes: Long? = null
    var failure: NetworkDiagnosticFailure? = null

    fun nextRequest(host: String, firstEvent: Int) {
        this.host = host
        this.firstEvent = firstEvent
        stage = null
        httpStatus = null
    }

    fun headers(response: HttpResponse) {
        host = response.request.url.host
        httpStatus = response.status.value
        stage = NetworkDiagnosticStage.BODY
    }
}

private class ProbeError(
    val failure: NetworkDiagnosticFailure,
    val stage: NetworkDiagnosticStage?,
) : Exception()

private const val PROBE_TIMEOUT_MS = 14_000L
private const val MAX_WEB_REDIRECTS = 3
private const val MAX_IMAGE_BYTES = 1024 * 1024L
private const val MAX_TEXT_BYTES = 64 * 1024L
private val REDIRECT_STATUSES = setOf(301, 302, 303, 307, 308)
private val IMAGE_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/avif")
private val CHALLENGE_ELEMENT = Regex("""\bid\s*=\s*["'](?:challenge-form|challenge-running)["']""")

private fun Url.origin() = Triple(protocol, host.lowercase(), port)

private fun redirectFrom(current: Url, location: String): Url? {
    val destination = runCatching {
        URLBuilder().takeFrom(current).apply {
            parameters.clear()
            takeFrom(location)
        }.build()
    }.getOrNull()
    return destination?.takeIf { it.protocol == URLProtocol.HTTPS && it.user == null && it.password == null }
}

private fun checkHttpFailure(status: Int) {
    val failure = when (status) {
        in 200..299 -> return
        407 -> NetworkDiagnosticFailure.PROXY_AUTH
        429 -> NetworkDiagnosticFailure.RATE_LIMITED
        else -> NetworkDiagnosticFailure.HTTP_STATUS
    }
    throw ProbeError(failure, if (status == 407) NetworkDiagnosticStage.PROXY else NetworkDiagnosticStage.HTTP)
}

private fun HttpResponse.mediaType() =
    headers[HttpHeaders.ContentType]?.substringBefore(';')?.trim()?.lowercase().orEmpty()

private suspend fun HttpResponse.readPrefix() =
    bodyAsChannel().readBuffer(MAX_TEXT_BYTES).readByteArray()

private fun isAuthenticationError(message: String) =
    message.startsWith("Error occurred at the OAuth process.", ignoreCase = true)

private fun isPixivPage(html: String): Boolean {
    val title = html.substringAfter("<title", "").substringAfter('>', "").substringBefore("</title>")
    // The real login page embeds Cloudflare's background script even when it serves normally.
    val interstitial = "just a moment" in title || CHALLENGE_ELEMENT.containsMatchIn(html)
    return !interstitial && ("pixiv" in title || "property=\"og:site_name\" content=\"pixiv\"" in html)
}

private fun hasImageSignature(type: String, prefix: ByteArray): Boolean = when (type) {
    "image/jpeg", "image/jpg" -> prefix.size >= 3 &&
        prefix[0] == 0xff.toByte() && prefix[1] == 0xd8.toByte() && prefix[2] == 0xff.toByte()
    "image/png" -> prefix.size >= 8 && prefix[0] == 0x89.toByte() &&
        prefix.matchesAscii("PNG\r\n\u001a\n", 1)
    "image/gif" -> prefix.matchesAscii("GIF87a") || prefix.matchesAscii("GIF89a")
    "image/webp" -> prefix.matchesAscii("RIFF") && prefix.matchesAscii("WEBP", 8)
    "image/avif" -> prefix.matchesAscii("ftypavif", 4) || prefix.matchesAscii("ftypavis", 4)
    else -> false
}

private fun ByteArray.matchesAscii(value: String, offset: Int = 0): Boolean =
    size >= offset + value.length && value.indices.all { this[offset + it] == value[it].code.toByte() }
