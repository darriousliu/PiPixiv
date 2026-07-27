package com.mrl.pixiv.common.network

import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.data.Constants.API_HOST
import com.mrl.pixiv.common.data.Constants.AUTH_HOST
import com.mrl.pixiv.common.data.Constants.IMAGE_HOST
import com.mrl.pixiv.common.data.Constants.STATIC_IMAGE_HOST
import com.mrl.pixiv.common.serialize.JSON
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InterruptedIOException
import java.net.InetAddress
import java.net.Proxy
import java.net.Socket
import java.net.SocketException
import java.net.UnknownHostException
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Serializable
private data class DnsJsonResponse(
    @SerialName("Status")
    val status: Int = -1,
    @SerialName("Answer")
    val answers: List<DnsJsonAnswer> = emptyList(),
)

@Serializable
private data class DnsJsonAnswer(
    val data: String,
    val type: Int,
)

fun OkHttpClient.Builder.bypassSNI(
    queryUrl: String,
    nonStrictSSL: Boolean,
    fallback: Map<String, String>,
    dohTimeout: Int,
): OkHttpClient.Builder {
    val dohResolver = DnsJsonResolver(
        queryUrl = queryUrl,
        timeoutSeconds = dohTimeout,
        unsafeSSL = nonStrictSSL,
    )
    val originResolver = RacingOriginDnsResolver(
        timeoutSeconds = dohTimeout,
        dohLookup = dohResolver::lookup,
        systemLookup = Dns.SYSTEM::lookup,
    )
    return dns(
        SniReplaceDns(
            fallback = fallback,
            originLookup = originResolver::lookup,
        )
    ).proxy(
        Proxy.NO_PROXY
    ).sslSocketFactory(
        SniReplacingSslSocketFactory(SystemTls.socketFactory),
        SystemTls.trustManager,
    )
}

internal class SniReplaceDns(
    private val fallback: Map<String, String>,
    private val originLookup: (String) -> DnsLookupResult?,
    private val systemDns: Dns = Dns.SYSTEM,
    private val fallbackLookup: (String) -> List<InetAddress> = ::parseFallbackAddresses,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val normalizedHostname = hostname.lowercase()
        val fallbackHost = fallback.entries
            .firstOrNull { (host) -> host.equals(normalizedHostname, ignoreCase = true) }
            ?.value
        val isManagedHost = normalizedHostname in PIXIV_BYPASS_HOSTS || fallbackHost != null
        if (!isManagedHost) {
            return systemDns.lookup(hostname).logResult(hostname, "system DNS")
        }

        val originHostname = PIXIV_ORIGIN_ALIASES[normalizedHostname] ?: hostname
        val originResult = lookupResultSafely("origin DNS", hostname) {
            originLookup(originHostname)
        }
        val fallbackAddresses = fallbackHost?.let {
            lookupSafely("fallback", hostname) {
                fallbackLookup(it)
            }
        }.orEmpty()
        val bypassAddresses = (originResult?.addresses.orEmpty() + fallbackAddresses).distinct()
        if (bypassAddresses.isNotEmpty()) {
            val source = when {
                originResult != null && fallbackAddresses.isNotEmpty() ->
                    "${originResult.source} + fallback"
                originResult != null -> originResult.source
                else -> "fallback"
            }
            return bypassAddresses.logResult(hostname, source)
        }

        throw UnknownHostException("No bypass addresses found for $hostname")
    }

    private fun lookupSafely(
        source: String,
        hostname: String,
        block: () -> List<InetAddress>,
    ): List<InetAddress> = try {
        block()
    } catch (e: Exception) {
        Logger.w(e) { "$source lookup failed for $hostname" }
        emptyList()
    }

    private fun lookupResultSafely(
        source: String,
        hostname: String,
        block: () -> DnsLookupResult?,
    ): DnsLookupResult? = try {
        block()
    } catch (e: Exception) {
        if (Thread.currentThread().isInterrupted) {
            throw e
        }
        Logger.w(e) { "$source lookup failed for $hostname" }
        null
    }

    private fun List<InetAddress>.logResult(
        hostname: String,
        source: String,
    ): List<InetAddress> = distinct().also {
        Logger.d { "DNS lookup $hostname via $source result: $it" }
    }
}

internal data class DnsLookupResult(
    val source: String,
    val addresses: List<InetAddress>,
)

internal class RacingOriginDnsResolver(
    timeoutSeconds: Int,
    private val dohLookup: (String) -> List<InetAddress>,
    private val systemLookup: (String) -> List<InetAddress>,
    private val executor: ExecutorService = OriginDnsExecutor.instance,
) {
    private val timeoutSeconds = timeoutSeconds.coerceAtLeast(MIN_DOH_TIMEOUT_SECONDS)

    fun lookup(hostname: String): DnsLookupResult? {
        val tasks = listOf(
            addressLookupTask("DoH($hostname)") {
                dohLookup(hostname)
            },
            addressLookupTask("system origin DNS($hostname)") {
                systemLookup(hostname)
            },
        )
        return try {
            executor.invokeAny(
                tasks,
                timeoutSeconds.toLong(),
                TimeUnit.SECONDS,
            )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw UnknownHostException("Origin DNS lookup interrupted for $hostname").apply {
                initCause(e)
            }
        } catch (e: TimeoutException) {
            Logger.w(e) { "Origin DNS lookup timed out for $hostname" }
            null
        } catch (e: ExecutionException) {
            Logger.w(e.cause ?: e) { "All origin DNS lookups failed for $hostname" }
            null
        }
    }

    private fun addressLookupTask(
        source: String,
        lookup: () -> List<InetAddress>,
    ) = Callable {
        try {
            val addresses = lookup().distinct()
            if (addresses.isEmpty()) {
                throw UnknownHostException("$source returned no addresses")
            }
            DnsLookupResult(source, addresses)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        } catch (e: InterruptedIOException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "$source lookup failed" }
            throw e
        }
    }
}

internal class DnsJsonResolver(
    private val queryUrl: String,
    private val timeoutSeconds: Int,
    unsafeSSL: Boolean,
) {
    private val endpoint by lazy {
        queryUrl.toHttpUrl()
    }
    private val client by lazy {
        OkHttpClient.Builder().apply {
            if (unsafeSSL) {
                ignoreSSL()
            }
            callTimeout(
                timeoutSeconds.coerceAtLeast(MIN_DOH_TIMEOUT_SECONDS)
                    .seconds
                    .toJavaDuration()
            )
        }.build()
    }

    fun lookup(hostname: String): List<InetAddress> {
        val url = endpoint.newBuilder()
            .setQueryParameter("name", hostname)
            .setQueryParameter("type", "A")
            .build()
        return client.newCall(
            Request.Builder()
                .url(url)
                .header("Accept", "application/dns-json")
                .build()
        ).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("DoH request failed with HTTP ${response.code}")
            }
            decodeDnsJsonAddresses(response.body.string())
        }
    }
}

internal fun decodeDnsJsonAddresses(body: String): List<InetAddress> {
    val response = JSON.decodeFromString<DnsJsonResponse>(body)
    if (response.status != 0) {
        throw UnknownHostException("DoH query failed with status ${response.status}")
    }
    return response.answers.asSequence()
        .filter { it.type == DNS_RECORD_TYPE_A }
        .mapNotNull { parseIpv4Address(it.data) }
        .distinct()
        .toList()
}

internal fun parseIpv4Address(value: String): InetAddress? {
    val octets = value.split('.')
    if (octets.size != IPV4_OCTET_COUNT) return null

    val address = ByteArray(IPV4_OCTET_COUNT)
    octets.forEachIndexed { index, octet ->
        if (octet.isEmpty() || octet.any { !it.isDigit() }) return null
        val number = octet.toIntOrNull() ?: return null
        if (number !in 0..IPV4_MAX_OCTET) return null
        address[index] = number.toByte()
    }
    return InetAddress.getByAddress(address)
}

internal fun parseFallbackAddresses(value: String): List<InetAddress> =
    value.split(FALLBACK_ADDRESS_SEPARATOR)
        .mapNotNull(::parseIpv4Address)
        .distinct()

internal class SniReplacingSslSocketFactory(
    private val delegate: SSLSocketFactory,
) : SSLSocketFactory() {
    override fun getDefaultCipherSuites(): Array<String> =
        delegate.defaultCipherSuites

    override fun getSupportedCipherSuites(): Array<String> =
        delegate.supportedCipherSuites

    override fun createSocket(): Socket =
        delegate.createSocket()

    override fun createSocket(
        socket: Socket?,
        host: String?,
        port: Int,
        autoClose: Boolean,
    ): Socket {
        val rawSocket = socket ?: throw SocketException("Socket is null")
        val peerAddress = rawSocket.inetAddress
            ?: throw SocketException("Socket is not connected")
        val peerHost = when (host?.lowercase()) {
            in PIXIV_SNI_REPLACEMENT_HOSTS -> PIXIV_TLS_SERVER_NAME
            in PIXIV_NO_SNI_HOSTS -> peerAddress.hostAddress
            else -> host ?: peerAddress.hostAddress
        }
        return delegate.createSocket(
            rawSocket,
            peerHost,
            port,
            autoClose,
        ).configureServerName(host)
    }

    override fun createSocket(host: String?, port: Int): Socket =
        delegate.createSocket(requireHost(host), port).configureServerName(host)

    override fun createSocket(
        host: String?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int,
    ): Socket = delegate.createSocket(
        requireHost(host),
        port,
        localAddress,
        localPort,
    ).configureServerName(host)

    override fun createSocket(address: InetAddress?, port: Int): Socket =
        delegate.createSocket(address, port)

    override fun createSocket(
        address: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int,
    ): Socket = delegate.createSocket(
        address,
        port,
        localAddress,
        localPort,
    )

    private fun requireHost(host: String?): String =
        host ?: throw UnknownHostException("Host is null")

    private fun Socket.configureServerName(host: String?): Socket {
        val sslSocket = this as? SSLSocket
            ?: throwAndClose(SocketException("Delegate did not create an SSLSocket"))
        val serverNames = when (host?.lowercase()) {
            in PIXIV_SNI_REPLACEMENT_HOSTS -> listOf(SNIHostName(PIXIV_TLS_SERVER_NAME))
            in PIXIV_NO_SNI_HOSTS -> emptyList()
            else -> return sslSocket
        }
        return try {
            sslParameters = sslParameters.apply {
                this.serverNames = serverNames
            }
            sslSocket
        } catch (failure: Throwable) {
            throwAndClose(failure)
        }
    }

    private fun Socket.throwAndClose(failure: Throwable): Nothing {
        try {
            close()
        } catch (closeFailure: Throwable) {
            failure.addSuppressed(closeFailure)
        }
        throw failure
    }
}

fun OkHttpClient.Builder.ignoreSSL() {
    val sslContext = SSLContext.getInstance("TLS")
    val trust = object : X509TrustManager {
        override fun checkClientTrusted(
            chain: Array<out X509Certificate>?,
            authType: String?,
        ) = Unit

        override fun checkServerTrusted(
            chain: Array<out X509Certificate>?,
            authType: String?,
        ) = Unit

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
    sslContext.init(null, arrayOf(trust), SecureRandom())

    sslSocketFactory(sslContext.socketFactory, trust)
    hostnameVerifier { _, _ -> true }
}

private object SystemTls {
    private val trustManagers = TrustManagerFactory
        .getInstance(TrustManagerFactory.getDefaultAlgorithm())
        .apply { init(null as KeyStore?) }
        .trustManagers

    val trustManager: X509TrustManager = trustManagers.singleOrNull() as? X509TrustManager
        ?: error("Unexpected default trust managers: ${trustManagers.contentToString()}")

    val socketFactory: SSLSocketFactory = SSLContext.getInstance("TLS")
        .apply { init(null, arrayOf(trustManager), null) }
        .socketFactory
}

private object OriginDnsExecutor {
    val instance: ExecutorService = Executors.newFixedThreadPool(MAX_ORIGIN_DNS_THREADS) { task ->
        Thread(task, "PiPixiv-origin-dns").apply {
            isDaemon = true
        }
    }
}

private const val DNS_RECORD_TYPE_A = 1
private const val IPV4_OCTET_COUNT = 4
private const val IPV4_MAX_OCTET = 255
private const val MIN_DOH_TIMEOUT_SECONDS = 1
private const val MAX_ORIGIN_DNS_THREADS = 8
private val FALLBACK_ADDRESS_SEPARATOR = Regex("[,\\s]+")
internal const val PIXIV_ORIGIN_HOST = "pixiv.net"
internal const val PIXIV_ORIGIN_ALIAS = "$PIXIV_ORIGIN_HOST.cdn.cloudflare.net"
internal const val PIXIV_TLS_SERVER_NAME = "pixiv.me"

private val PIXIV_SNI_REPLACEMENT_HOSTS = setOf(API_HOST, AUTH_HOST)
private val PIXIV_NO_SNI_HOSTS = setOf(IMAGE_HOST, STATIC_IMAGE_HOST)
private val PIXIV_BYPASS_HOSTS =
    PIXIV_SNI_REPLACEMENT_HOSTS + PIXIV_NO_SNI_HOSTS
private val PIXIV_ORIGIN_ALIASES = mapOf(
    API_HOST to PIXIV_ORIGIN_ALIAS,
    AUTH_HOST to PIXIV_ORIGIN_ALIAS,
    IMAGE_HOST to "$IMAGE_HOST.cdn.cloudflare.net",
    STATIC_IMAGE_HOST to "$STATIC_IMAGE_HOST.cdn.cloudflare.net",
)
