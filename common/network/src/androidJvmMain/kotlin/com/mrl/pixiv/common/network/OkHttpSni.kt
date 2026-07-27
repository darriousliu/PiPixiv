package com.mrl.pixiv.common.network

import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.serialize.JSON
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress
import java.net.Socket
import java.net.SocketException
import java.net.UnknownHostException
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
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
    return dns(
        SniReplaceDns(
            fallback = fallback,
            dohLookup = dohResolver::lookup,
        )
    ).sslSocketFactory(
        NoSniSslSocketFactory(SystemTls.socketFactory),
        SystemTls.trustManager,
    )
}

internal class SniReplaceDns(
    private val fallback: Map<String, String>,
    private val dohLookup: (String) -> List<InetAddress>,
    private val systemDns: Dns = Dns.SYSTEM,
    private val fallbackLookup: (String) -> List<InetAddress> = {
        InetAddress.getAllByName(it).toList()
    },
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val candidates = linkedSetOf<InetAddress>()

        fallback[hostname]?.let { fallbackHost ->
            candidates += lookupSafely("fallback", hostname) {
                fallbackLookup(fallbackHost)
            }
        }
        candidates += lookupSafely("DoH", hostname) {
            dohLookup(hostname)
        }
        candidates += lookupSafely("system DNS", hostname) {
            systemDns.lookup(hostname)
        }

        if (candidates.isEmpty()) {
            throw UnknownHostException("No addresses found for $hostname")
        }

        return candidates.toList().also {
            Logger.d { "DNS lookup $hostname result: $it" }
        }
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
            callTimeout(timeoutSeconds.seconds.toJavaDuration())
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

internal class NoSniSslSocketFactory(
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
        return delegate.createSocket(
            rawSocket,
            peerAddress.hostAddress,
            port,
            autoClose,
        ).withoutSni()
    }

    override fun createSocket(host: String?, port: Int): Socket =
        delegate.createSocket(resolveAddress(host), port).withoutSni()

    override fun createSocket(
        host: String?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int,
    ): Socket = delegate.createSocket(
        resolveAddress(host),
        port,
        localAddress,
        localPort,
    ).withoutSni()

    override fun createSocket(address: InetAddress?, port: Int): Socket =
        delegate.createSocket(stripHost(address), port).withoutSni()

    override fun createSocket(
        address: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int,
    ): Socket = delegate.createSocket(
        stripHost(address),
        port,
        localAddress,
        localPort,
    ).withoutSni()

    private fun resolveAddress(host: String?): InetAddress {
        val resolvedHost = host ?: throw UnknownHostException("Host is null")
        return stripHost(InetAddress.getByName(resolvedHost))
    }

    private fun stripHost(address: InetAddress?): InetAddress {
        val resolvedAddress = address ?: throw UnknownHostException("Address is null")
        return if (resolvedAddress is Inet6Address) {
            Inet6Address.getByAddress(null, resolvedAddress.address, resolvedAddress.scopeId)
        } else {
            InetAddress.getByAddress(resolvedAddress.address)
        }
    }

    private fun Socket.withoutSni(): Socket {
        val sslSocket = this as? SSLSocket
            ?: throwAndClose(SocketException("Delegate did not create an SSLSocket"))
        return try {
            sslParameters = sslParameters.apply {
                serverNames = emptyList()
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

private const val DNS_RECORD_TYPE_A = 1
private const val IPV4_OCTET_COUNT = 4
private const val IPV4_MAX_OCTET = 255
