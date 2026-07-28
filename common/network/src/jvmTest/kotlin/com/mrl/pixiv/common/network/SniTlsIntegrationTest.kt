package com.mrl.pixiv.common.network

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.net.InetAddress
import java.net.Proxy
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SniTlsIntegrationTest {
    @Test
    fun apiClientHelloUsesReplacementSniAndKeepsOriginalHttpHost() {
        withTlsServer(API_HOST) { server, client ->
            val request = execute(server, client, API_HOST)

            assertEquals(listOf(PIXIV_TLS_SERVER_NAME), request.handshakeServerNames)
            assertEquals(API_HOST, request.url.host)
            assertEquals(server.port, request.url.port)
        }
    }

    @Test
    fun oauthClientHelloUsesReplacementSniAndKeepsOriginalHttpHost() {
        withTlsServer(AUTH_HOST) { server, client ->
            val request = execute(server, client, AUTH_HOST)

            assertEquals(listOf(PIXIV_TLS_SERVER_NAME), request.handshakeServerNames)
            assertEquals(AUTH_HOST, request.url.host)
            assertEquals(server.port, request.url.port)
        }
    }

    @Test
    fun imageClientHelloOmitsSniAndKeepsStrictHostnameVerification() {
        withTlsServer(IMAGE_HOST) { server, client ->
            val request = execute(server, client, IMAGE_HOST)

            assertTrue(request.handshakeServerNames.isEmpty())
            assertEquals(IMAGE_HOST, request.url.host)
            assertEquals(server.port, request.url.port)
        }
    }

    @Test
    fun staticImageClientHelloOmitsSniAndKeepsStrictHostnameVerification() {
        withTlsServer(STATIC_IMAGE_HOST) { server, client ->
            val request = execute(server, client, STATIC_IMAGE_HOST)

            assertTrue(request.handshakeServerNames.isEmpty())
            assertEquals(STATIC_IMAGE_HOST, request.url.host)
            assertEquals(server.port, request.url.port)
        }
    }

    @Test
    fun unknownHostKeepsStandardSni() {
        withTlsServer(UNKNOWN_HOST) { server, client ->
            val request = execute(server, client, UNKNOWN_HOST)

            assertEquals(listOf(UNKNOWN_HOST), request.handshakeServerNames)
            assertEquals(UNKNOWN_HOST, request.url.host)
            assertEquals(server.port, request.url.port)
        }
    }

    @Test
    fun replacementSniRejectsCertificateThatOnlyCoversReplacementHost() {
        withTlsServer(PIXIV_TLS_SERVER_NAME) { server, client ->
            assertFailsWith<SSLPeerUnverifiedException> {
                client.newCall(
                    Request.Builder()
                        .url(server.url("/").newBuilder().host(API_HOST).build())
                        .build()
                ).execute().use { }
            }
        }
    }

    @Test
    fun replacementSniStillRejectsUntrustedCertificateChains() {
        withTlsServer(
            certificateHost = API_HOST,
            trustServerCertificate = false,
        ) { server, client ->
            assertFailsWith<SSLHandshakeException> {
                client.newCall(
                    Request.Builder()
                        .url(server.url("/").newBuilder().host(API_HOST).build())
                        .build()
                ).execute().use { }
            }
        }
    }

    private fun execute(
        server: MockWebServer,
        client: OkHttpClient,
        host: String,
    ) = client.newCall(
        Request.Builder()
            .url(server.url("/").newBuilder().host(host).build())
            .build()
    ).execute().use {
        assertTrue(it.isSuccessful)
        server.takeRequest()
    }

    private fun withTlsServer(
        certificateHost: String,
        trustServerCertificate: Boolean = true,
        block: (MockWebServer, OkHttpClient) -> Unit,
    ) {
        val heldCertificate = HeldCertificate.Builder()
            .commonName("PiPixiv SNI test")
            .addSubjectAlternativeName(certificateHost)
            .build()
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(heldCertificate)
            .build()
        val clientCertificates = HandshakeCertificates.Builder().apply {
            if (trustServerCertificate) {
                addTrustedCertificate(heldCertificate.certificate)
            } else {
                addTrustedCertificate(
                    HeldCertificate.Builder()
                        .commonName("Unrelated SNI test certificate")
                        .build()
                        .certificate
                )
            }
        }.build()

        MockWebServer().use { server ->
            server.useHttps(serverCertificates.sslSocketFactory())
            server.enqueue(MockResponse())
            server.start()

            val client = OkHttpClient.Builder()
                .proxy(Proxy.NO_PROXY)
                .dns(Dns { listOf(InetAddress.getLoopbackAddress()) })
                .sslSocketFactory(
                    SniReplacingSslSocketFactory(clientCertificates.sslSocketFactory()),
                    clientCertificates.trustManager,
                )
                .build()
            block(server, client)
            client.connectionPool.evictAll()
        }
    }

    private companion object {
        const val API_HOST = "app-api.pixiv.net"
        const val AUTH_HOST = "oauth.secure.pixiv.net"
        const val IMAGE_HOST = "i.pximg.net"
        const val STATIC_IMAGE_HOST = "s.pximg.net"
        const val UNKNOWN_HOST = "unrelated.example"
    }
}
