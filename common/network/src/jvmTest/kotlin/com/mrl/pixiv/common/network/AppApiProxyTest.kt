package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.Constants.API_HOST
import com.mrl.pixiv.common.data.Constants.AUTH_HOST
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Protocol
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.net.InetAddress
import java.net.Proxy
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class AppApiProxyTest {
    @Test
    fun proxyHostValidationAndRewritingKeepTheOriginalEncodedUrl() {
        assertEquals("proxy.example", normalizeAppApiProxyHost(" proxy.example "))
        assertNull(normalizeAppApiProxyHost("proxy.example:8443"))
        assertNull(normalizeAppApiProxyHost("proxy.example/path"))
        assertNull(normalizeAppApiProxyHost("proxy.example?query=value"))
        assertNull(normalizeAppApiProxyHost("user:secret@proxy.example"))
        assertNull(normalizeAppApiProxyHost("${AUTH_HOST.uppercase()}."))

        val page = "https://$API_HOST:8443/v1/illust%20name?cursor=a%2Bb%2Fc&tag=x&tag=y"
        assertEquals(
            "https://proxy.example:8443/v1/illust%20name?cursor=a%2Bb%2Fc&tag=x&tag=y",
            resolveAppApiUrl(page, "proxy.example"),
        )
        assertEquals(page, resolveAppApiUrl(page, ""))
        for (url in listOf(
            "https://$AUTH_HOST/auth/token",
            "https://i.pximg.net/image.jpg",
            "https://$API_HOST.example/v1/illust",
        )) {
            assertEquals(url, resolveAppApiUrl(url, "proxy.example"))
        }
    }

    @Test
    fun changingOnlyTheApiHostReusesTheClientAndPreservesTheInFlightRoute() = runBlocking {
        val loopback = InetAddress.getByName("127.0.0.1")
        val certificate = HeldCertificate.Builder()
            .addSubjectAlternativeName(API_HOST)
            .addSubjectAlternativeName("localhost")
            .addSubjectAlternativeName("127.0.0.1")
            .build()
        val serverCertificates = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientCertificates = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()

        MockWebServer().use { server ->
            server.protocols = listOf(Protocol.HTTP_1_1)
            server.useHttps(serverCertificates.sslSocketFactory())
            server.start(loopback, 0)
            val originalUrl = "https://$API_HOST:${server.port}/v1/illust/ranking?cursor=a%2Bb%2Fc"
            listOf("old", "new", "official", "nested").forEach { server.enqueue(MockResponse(body = it)) }

            var settings = NetworkSettings(BypassSetting.Direct, "i.pximg.net", appApiProxyHost = "localhost")
            var clientCreations = 0
            HttpClient(OkHttp) {
                engine {
                    config {
                        proxy(Proxy.NO_PROXY)
                        protocols(listOf(Protocol.HTTP_1_1))
                        sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)
                        dns { host ->
                            if (host !in setOf(API_HOST, "localhost", "127.0.0.1")) {
                                throw UnknownHostException(host)
                            }
                            listOf(loopback)
                        }
                    }
                }
            }.applyAppApiRequests(
                readProxyHost = { (currentNetworkPreference() ?: settings).appApiProxyHost },
                readAccessToken = { "fixture-access-token" },
            ).use { client ->
                assertFailsWith<IllegalArgumentException> {
                    client.get("https://$AUTH_HOST:${server.port}/auth/token")
                }
                val clients = RoutedHttpClient(
                    readSetting = { settings.bypassSetting },
                    create = {
                        clientCreations++
                        client
                    },
                    close = HttpClient::close,
                    readPreference = { settings },
                )
                withTimeout(10.seconds) {
                    val responseStarted = CompletableDeferred<Unit>()
                    val release = CompletableDeferred<Unit>()
                    val old = async {
                        clients.withClient { routed ->
                            routed.prepareGet(originalUrl).execute { response ->
                                responseStarted.complete(Unit)
                                release.await()
                                response.bodyAsText() to clients.withClient { it.get(originalUrl).bodyAsText() }
                            }
                        }
                    }

                    responseStarted.await()
                    settings = settings.copy(appApiProxyHost = "127.0.0.1")
                    assertEquals("new", clients.withClient { it.get(originalUrl).bodyAsText() })
                    settings = settings.copy(appApiProxyHost = "")
                    assertEquals("official", clients.withClient { it.get(originalUrl).bodyAsText() })
                    release.complete(Unit)
                    assertEquals("old" to "nested", old.await())
                }

                val requests = List(4) { assertNotNull(server.takeRequest(2, TimeUnit.SECONDS)) }
                assertEquals(listOf("localhost", "127.0.0.1", API_HOST, "localhost"), requests.map { it.url.host })
                for (request in requests) {
                    assertEquals("GET", request.method)
                    assertEquals("/v1/illust/ranking?cursor=a%2Bb%2Fc", request.target)
                    assertEquals("Bearer fixture-access-token", request.headers[HttpHeaders.Authorization])
                }
                assertEquals(1, clientCreations)
            }
        }
    }
}
