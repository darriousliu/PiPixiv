package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.Constants.API_HOST
import com.mrl.pixiv.common.data.Constants.AUTH_HOST
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PixivEchTlsTest {
    @Test
    fun apiAndOauthClientHelloEncryptOriginalHostname() {
        for (host in listOf(API_HOST, AUTH_HOST)) {
            val hello = captureClientHello(host)
            val extensions = DataInputStream(ByteArrayInputStream(hello)).run {
                skipBytes(4 + 2 + 32)
                skipBytes(readUnsignedByte())
                skipBytes(readUnsignedShort())
                skipBytes(readUnsignedByte())
                ByteArray(readUnsignedShort()).also(::readFully)
            }
            val input = DataInputStream(ByteArrayInputStream(extensions))
            var serverName: String? = null
            var hasEch = false
            while (input.available() > 0) {
                val type = input.readUnsignedShort()
                val data = ByteArray(input.readUnsignedShort()).also(input::readFully)
                when (type) {
                    0 -> serverName = DataInputStream(ByteArrayInputStream(data)).run {
                        readUnsignedShort()
                        readUnsignedByte()
                        ByteArray(readUnsignedShort()).also(::readFully)
                            .toString(Charsets.US_ASCII)
                    }
                    0xfe0d -> hasEch = true
                }
            }

            assertEquals("cloudflare-ech.com", serverName)
            assertTrue(hasEch)
            assertFalse(hello.toString(Charsets.ISO_8859_1).contains(host))
        }
    }

    @Test
    fun apiWithoutEchConfigFailsBeforeSendingUnencryptedHostname() {
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            connectedTlsSocket(server, API_HOST).use { socket ->
                val failure = assertFailsWith<SSLException> { socket.startHandshake() }

                assertTrue(failure.message.orEmpty().contains("No ECH config provided"))
            }
        }
    }

    @Test
    fun nonApiHostsKeepCertificateChainAndHostnameVerification() {
        val certificate = HeldCertificate.Builder()
            .addSubjectAlternativeName(TEST_HOST)
            .build()
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(certificate)
            .build()
        val systemTrust = HandshakeCertificates.Builder()
            .addTrustedCertificate(certificate.certificate)
            .build()
        val tls = PixivEchTlsContext(systemTrust.trustManager)

        MockWebServer().use { server ->
            server.useHttps(serverCertificates.sslSocketFactory())
            server.enqueue(MockResponse())
            server.start()
            val client = OkHttpClient.Builder()
                .proxy(Proxy.NO_PROXY)
                .dns(Dns { listOf(InetAddress.getLoopbackAddress()) })
                .sslSocketFactory(tls.socketFactory, tls.trustManager)
                .build()
            try {
                client.newCall(request(server, TEST_HOST)).execute().use { response ->
                    assertTrue(response.isSuccessful)
                }
                assertEquals(TEST_HOST, server.takeRequest().url.host)
                assertFailsWith<SSLPeerUnverifiedException> {
                    client.newCall(request(server, "wrong.example")).execute().close()
                }
                val untrustedClient = client.newBuilder()
                    .sslSocketFactory(PixivEchTls.socketFactory, PixivEchTls.trustManager)
                    .build()
                try {
                    assertFailsWith<SSLHandshakeException> {
                        untrustedClient.newCall(request(server, TEST_HOST)).execute().close()
                    }
                } finally {
                    untrustedClient.connectionPool.evictAll()
                }
            } finally {
                client.connectionPool.evictAll()
            }
        }
    }

    private fun captureClientHello(host: String): ByteArray {
        val executor = Executors.newSingleThreadExecutor()
        try {
            ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
                server.soTimeout = TIMEOUT_MILLIS
                val captured = executor.submit<ByteArray> {
                    server.accept().use { socket ->
                        socket.soTimeout = TIMEOUT_MILLIS
                        DataInputStream(socket.getInputStream()).run {
                            assertEquals(22, readUnsignedByte())
                            readUnsignedShort()
                            ByteArray(readUnsignedShort()).also(::readFully)
                        }
                    }
                }
                connectedTlsSocket(server, host).use { socket ->
                    PixivEchTls.configure(socket, ECH_CONFIG)
                    // 抓取 ClientHello 后服务端关闭连接，不需要真实 ECH 服务或网络。
                    assertFailsWith<SSLException> { socket.startHandshake() }
                }
                return captured.get(TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun connectedTlsSocket(server: ServerSocket, host: String): SSLSocket {
        val rawSocket = Socket(Proxy.NO_PROXY).apply {
            connect(InetSocketAddress(InetAddress.getLoopbackAddress(), server.localPort))
            soTimeout = TIMEOUT_MILLIS
        }
        return (PixivEchTls.socketFactory.createSocket(
            rawSocket,
            host,
            server.localPort,
            true,
        ) as SSLSocket).apply {
            soTimeout = TIMEOUT_MILLIS
        }
    }

    private fun request(server: MockWebServer, host: String): Request = Request.Builder()
        .url(server.url("/").newBuilder().host(host).build())
        .build()

    private companion object {
        const val TEST_HOST = "unrelated.example"
        const val TIMEOUT_MILLIS = 5_000

        // 公开 ECH 配置作为离线握手样本，不依赖在线 DNS 或其有效期。
        val ECH_CONFIG: ByteArray = Base64.getDecoder().decode(
            "AEX+DQBBMAAgACCpWXjgfCszTextDRaGYJ7Pmcjt97+vFUzo9eu5OxjAFQAEAAEAAQAS" +
                "Y2xvdWRmbGFyZS1lY2guY29tAAA="
        )
    }
}
