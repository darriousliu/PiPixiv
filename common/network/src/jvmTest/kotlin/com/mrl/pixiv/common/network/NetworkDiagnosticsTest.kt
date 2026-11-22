package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.Constants
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting.Proxy.ProxyType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import okhttp3.Protocol
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okio.Buffer
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class NetworkDiagnosticsTest {
    @Test
    fun appApiDiagnosisUsesTheReverseProxyWithoutChangingTheOtherProbes() = runBlocking {
        DiagnosticsOrigin().use { origin ->
            NetworkDiagnosticsProxyFixture(ProxyType.HTTP, origin.port).use { transportProxy ->
                val settings = NetworkSettings(
                    bypassSetting = transportProxy.setting,
                    imageHost = "localhost",
                    appApiProxyHost = "127.0.0.1",
                )
                val diagnostics = origin.diagnostics(
                    appApi = "https://${Constants.API_HOST}:${origin.port}/v1/illust/ranking?mode=day",
                )

                val results = runDiagnostics(diagnostics, settings)
                assertSuccessfulChecks(results, origin)
                assertObservedRoutes(results, NetworkDiagnosticRouteMode.HTTP, transportProxy.setting.port)
                assertEquals("127.0.0.1", results.getValue(NetworkDiagnosticTarget.APP_API).host)
                assertEquals(
                    listOf(
                        "127.0.0.1" to origin.port,
                        "localhost" to origin.port,
                        "localhost" to origin.port,
                        "localhost" to origin.port,
                    ),
                    transportProxy.destinations,
                )
                val requests = origin.requests(4)
                assertEquals(
                    listOf("/v1/illust/ranking?mode=day", "/web", "/login", "/image.png"),
                    requests.map { it.target },
                )
                assertTrue(requests.all { it.headers["Authorization"] == null && it.headers["Cookie"] == null })
            }
        }
    }

    @Test
    fun httpToSocksSwitchUsesNextRunsSettingsAndDownloadsTheWholeImageAnonymously() = runBlocking {
        DiagnosticsOrigin().use { origin ->
            NetworkDiagnosticsProxyFixture(ProxyType.HTTP, origin.port).use { httpProxy ->
                NetworkDiagnosticsProxyFixture(ProxyType.SOCKS, origin.port).use { socksProxy ->
                    var preference = UserPreference(
                        bypassSetting = httpProxy.setting,
                        imageHost = "127.0.0.1",
                    )
                    var tokenReads = 0
                    val application = startKoin {
                        modules(module {
                            single<NetworkFeature> {
                                object : NetworkFeature {
                                    override fun provideUserPreference() = preference

                                    override suspend fun provideUserAccessToken(): String {
                                        tokenReads++
                                        return "signed-in-account"
                                    }
                                }
                            }
                        })
                    }
                    try {
                        val diagnostics = origin.diagnostics()
                        val first = runDiagnostics(diagnostics, preference.networkSettings()) {
                            if (it.target == NetworkDiagnosticTarget.APP_API) {
                                preference = preference.copy(
                                    bypassSetting = socksProxy.setting,
                                    imageHost = "localhost",
                                )
                            }
                        }
                        assertSuccessfulChecks(first, origin)
                        assertEquals("127.0.0.1", first.getValue(NetworkDiagnosticTarget.IMAGE).host)
                        assertObservedRoutes(first, NetworkDiagnosticRouteMode.HTTP, httpProxy.setting.port)
                        assertTrue(httpProxy.destinations.contains("localhost" to origin.port))
                        assertTrue(httpProxy.destinations.contains("127.0.0.1" to origin.port))
                        assertTrue(socksProxy.destinations.isEmpty())

                        val httpDestinations = httpProxy.destinations
                        val second = runDiagnostics(diagnostics, preference.networkSettings())
                        assertSuccessfulChecks(second, origin)
                        assertEquals("localhost", second.getValue(NetworkDiagnosticTarget.IMAGE).host)
                        assertObservedRoutes(second, NetworkDiagnosticRouteMode.SOCKS, socksProxy.setting.port)
                        assertTrue(socksProxy.destinations.isNotEmpty())
                        assertTrue(socksProxy.destinations.all { it == "localhost" to origin.port })
                        assertEquals(httpDestinations, httpProxy.destinations)
                        assertEquals(0, tokenReads)
                        assertEquals("signed-in-account", application.koin.get<NetworkFeature>().provideUserAccessToken())
                        assertEquals(1, tokenReads)
                        for (result in listOf(first, second).flatMap { it.values }) {
                            assertTrue(result.events.none {
                                it.stage == NetworkDiagnosticStage.DNS && it.host == "localhost"
                            }, "A proxy should receive the origin name without local origin DNS")
                        }

                        val received = origin.requests(8)
                        val expectedPaths = listOf("/api", "/web", "/login", "/image.png")
                        assertEquals(expectedPaths + expectedPaths, received.map { it.url.encodedPath })
                        received.forEach { request ->
                            assertEquals(null, request.headers["Authorization"])
                            assertEquals(null, request.headers["Cookie"])
                        }
                        val images = received.filter { it.url.encodedPath == "/image.png" }
                        assertEquals(listOf("127.0.0.1", "localhost"), images.map { it.url.host })
                        assertTrue(images.all { it.headers["Referer"] == Constants.PIXIV_API_BASE_URL })
                    } finally {
                        stopKoin()
                    }
                }
            }
        }
    }

    @Test
    fun brokenProxiesNeverFallBackToTheReachableOrigin() = runBlocking {
        DiagnosticsOrigin().use { origin ->
            NetworkDiagnosticsProxyFixture(ProxyType.HTTP, origin.port, reject = true).use { httpProxy ->
                NetworkDiagnosticsProxyFixture(ProxyType.SOCKS, origin.port, reject = true).use { socksProxy ->
                    val diagnostics = origin.diagnostics()
                    val direct = runDiagnostics(diagnostics, NetworkSettings(BypassSetting.Direct, "localhost"))
                    assertSuccessfulChecks(direct, origin)
                    assertObservedRoutes(direct, NetworkDiagnosticRouteMode.DIRECT, origin.port)
                    assertTrue(direct.getValue(NetworkDiagnosticTarget.APP_API).events.any {
                        it.stage == NetworkDiagnosticStage.DNS &&
                            it.status == NetworkDiagnosticEventStatus.SUCCEEDED &&
                            it.host == "localhost" &&
                            "127.0.0.1" in it.addresses &&
                            NetworkDiagnosticResolver.SYSTEM in it.resolvers
                    })
                    assertEquals(4, origin.requestCount)

                    val http = runDiagnostics(diagnostics, NetworkSettings(httpProxy.setting, "localhost"))
                    assertEquals(4, http.size)
                    assertEquals(4, httpProxy.destinations.size)
                    http.values.forEach { result ->
                        assertEquals(NetworkDiagnosticStatus.FAILED, result.status)
                        assertEquals(NetworkDiagnosticFailure.PROXY_AUTH, result.failure)
                        assertEquals(NetworkDiagnosticStage.PROXY, result.failureStage)
                    }

                    val socks = runDiagnostics(diagnostics, NetworkSettings(socksProxy.setting, "localhost"))
                    assertEquals(4, socks.size)
                    assertEquals(4, socksProxy.destinations.size)
                    socks.values.forEach { result ->
                        assertEquals(NetworkDiagnosticStatus.FAILED, result.status)
                        assertEquals(NetworkDiagnosticFailure.TRANSPORT, result.failure)
                        assertEquals(NetworkDiagnosticStage.PROXY, result.failureStage)
                    }

                    val stoppedProxy = NetworkDiagnosticsProxyFixture(ProxyType.HTTP, origin.port).use { it.setting }
                    val disconnected = runDiagnostics(diagnostics, NetworkSettings(stoppedProxy, "localhost"))
                    assertEquals(4, disconnected.size)
                    disconnected.values.forEach { result ->
                        assertEquals(NetworkDiagnosticStatus.FAILED, result.status)
                        assertEquals(NetworkDiagnosticFailure.TRANSPORT, result.failure)
                        assertEquals(NetworkDiagnosticStage.CONNECT, result.failureStage)
                    }
                    assertEquals(4, origin.requestCount, "A direct fallback would reach this TLS server")
                }
            }
        }
    }

    @Test
    fun fakeSuccessPagesAndIncompleteImageBodiesAreReportedAsFailures() = runBlocking {
        DiagnosticsOrigin().use { origin ->
            NetworkDiagnosticsProxyFixture(ProxyType.HTTP, origin.port).use { proxy ->
                val diagnostics = origin.diagnostics()
                val settings = NetworkSettings(proxy.setting, "localhost")
                origin.webResponse = html("<html><title>Proxy verification required</title></html>")
                origin.imageResponse = MockResponse.Builder()
                    .setHeader("Content-Type", "image/png")
                    .body("<html>Proxy verification required</html>")
                    .build()

                val fakeResults = runDiagnostics(diagnostics, settings)
                val web = fakeResults.getValue(NetworkDiagnosticTarget.WEB)
                assertEquals(200, web.httpStatus)
                assertEquals(NetworkDiagnosticStatus.FAILED, web.status)
                assertEquals(NetworkDiagnosticFailure.UNEXPECTED_RESPONSE, web.failure)
                val fakeImage = fakeResults.getValue(NetworkDiagnosticTarget.IMAGE)
                assertEquals(200, fakeImage.httpStatus)
                assertEquals(NetworkDiagnosticStatus.FAILED, fakeImage.status)
                assertEquals(NetworkDiagnosticStage.BODY, fakeImage.failureStage)
                assertEquals(NetworkDiagnosticFailure.UNEXPECTED_RESPONSE, fakeImage.failure)

                origin.imageResponse = MockResponse.Builder()
                    .body(Buffer().write(origin.png.copyOf(origin.png.size / 2)))
                    .setHeader("Content-Type", "image/png")
                    .setHeader("Content-Length", origin.png.size)
                    .onResponseEnd(SocketEffect.CloseSocket())
                    .build()

                val results = runDiagnostics(diagnostics, settings)
                val image = results.getValue(NetworkDiagnosticTarget.IMAGE)
                assertEquals(200, image.httpStatus)
                assertEquals(NetworkDiagnosticStatus.FAILED, image.status)
                assertEquals(NetworkDiagnosticStage.BODY, image.failureStage)
                assertEquals(NetworkDiagnosticFailure.INCOMPLETE_BODY, image.failure)
                assertEquals(
                    NetworkDiagnosticStatus.REQUIRES_LOGIN,
                    results.getValue(NetworkDiagnosticTarget.APP_API).status,
                )
                assertEquals(NetworkDiagnosticStatus.SUCCESS, results.getValue(NetworkDiagnosticTarget.LOGIN).status)
                assertEquals(8, origin.requestCount)
            }
        }
    }

    @Test
    fun webFollowsAllowedHttpsRedirectsAndReportsFailuresAgainstTheFinalTarget() = runBlocking {
        DiagnosticsOrigin().use { origin ->
            val settings = NetworkSettings(BypassSetting.Direct, "localhost")
            val diagnostics = origin.diagnostics()
            val webTarget = NetworkDiagnosticTarget.WEB
            origin.webResponse = redirect("/login")

            val relative = runDiagnostics(diagnostics, settings).getValue(webTarget)
            assertEquals(NetworkDiagnosticStatus.SUCCESS, relative.status, relative.toString())
            assertEquals(
                listOf("/api", "/web", "/login", "/login", "/image.png"),
                origin.requests(5).map { it.url.encodedPath },
            )

            val alias = "https://127.0.0.1:${origin.port}/login"
            val downgrade = "http://localhost:${origin.port}/login"
            for (location in listOf(alias, downgrade)) {
                origin.webResponse = redirect(location)
                val blocked = runDiagnostics(diagnostics, settings).getValue(webTarget)
                assertEquals(NetworkDiagnosticFailure.REDIRECT, blocked.failure, blocked.toString())
                assertEquals(NetworkDiagnosticStage.HTTP, blocked.failureStage)
                assertEquals(
                    listOf("/api", "/web", "/login", "/image.png"),
                    origin.requests(4).map { it.url.encodedPath },
                )
            }
            assertEquals(13, origin.requestCount)

            origin.webResponse = redirect(alias)
            origin.loginResponse = html(
                """<html><head><title>Pixiv</title></head><body><form id="challenge-form"></form></body></html>""",
            )
            val finalFailure = runDiagnostics(origin.diagnostics(loginHost = "127.0.0.1"), settings).getValue(webTarget)
            assertEquals("127.0.0.1", finalFailure.host)
            assertEquals(NetworkDiagnosticFailure.UNEXPECTED_RESPONSE, finalFailure.failure)
            assertEquals(NetworkDiagnosticStage.BODY, finalFailure.failureStage)
            assertTrue(finalFailure.events.any { it.host == "localhost" && it.httpStatus == 302 })
            assertTrue(finalFailure.events.any { it.host == "127.0.0.1" && it.httpStatus == 200 })
            assertEquals(18, origin.requestCount)
        }
    }

    private suspend fun runDiagnostics(
        diagnostics: NetworkDiagnostics,
        settings: NetworkSettings,
        onUpdate: (NetworkDiagnosticResult) -> Unit = {},
    ): Map<NetworkDiagnosticTarget, NetworkDiagnosticResult> = withTimeout(15.seconds) {
        buildMap {
            diagnostics.run(settings) { result ->
                put(result.target, result)
                onUpdate(result)
            }
        }
    }

    private fun assertSuccessfulChecks(
        results: Map<NetworkDiagnosticTarget, NetworkDiagnosticResult>,
        origin: DiagnosticsOrigin,
    ) {
        assertEquals(NetworkDiagnosticTarget.entries.toSet(), results.keys)
        val api = results.getValue(NetworkDiagnosticTarget.APP_API)
        assertEquals(NetworkDiagnosticStatus.REQUIRES_LOGIN, api.status, api.toString())
        assertEquals(400, api.httpStatus)
        listOf(NetworkDiagnosticTarget.WEB, NetworkDiagnosticTarget.LOGIN).forEach { target ->
            val web = results.getValue(target)
            assertEquals(NetworkDiagnosticStatus.SUCCESS, web.status, web.toString())
        }
        val image = results.getValue(NetworkDiagnosticTarget.IMAGE)
        assertEquals(NetworkDiagnosticStatus.SUCCESS, image.status, image.toString())
        assertEquals(origin.png.size.toLong(), image.downloadedBytes)
    }

    private fun assertObservedRoutes(
        results: Map<NetworkDiagnosticTarget, NetworkDiagnosticResult>,
        mode: NetworkDiagnosticRouteMode,
        port: Int,
    ) {
        val expected = NetworkDiagnosticRoute(mode, "127.0.0.1", port)
        results.values.forEach { result ->
            val observed = result.events.filter { it.status == NetworkDiagnosticEventStatus.SUCCEEDED }
                .mapNotNull { it.route }
                .toSet()
            assertEquals(setOf(expected), observed, result.toString())
        }
    }

    private class DiagnosticsOrigin : AutoCloseable {
        private val certificate = HeldCertificate.Builder()
            .commonName("network-diagnostics")
            .addSubjectAlternativeName("localhost")
            .addSubjectAlternativeName("127.0.0.1")
            .build()
        private val serverCertificates = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        private val trusted = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        val png: ByteArray = ByteArrayOutputStream().use { output ->
            val image = BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)
            image.setRGB(0, 0, 0x80A0FF)
            check(ImageIO.write(image, "png", output))
            output.toByteArray()
        }

        @Volatile
        var webResponse = html("<html><head><title>pixiv</title></head></html>")

        @Volatile
        var loginResponse = html(
            """
            <html><head><title>登录 | pixiv</title></head>
            <body><script src="/cdn-cgi/challenge-platform/scripts/jsd/main.js"></script></body></html>
            """.trimIndent(),
        )

        @Volatile
        var imageResponse = MockResponse.Builder()
            .addHeader("Content-Type", "image/png")
            .body(Buffer().write(png))
            .build()

        private val server = MockWebServer().apply {
            protocols = listOf(Protocol.HTTP_1_1)
            useHttps(serverCertificates.sslSocketFactory())
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when (request.url.encodedPath) {
                    "/api", "/v1/illust/ranking" -> MockResponse.Builder()
                        .code(400)
                        .addHeader("Content-Type", "application/json")
                        .body(
                            """
                            {"error": {
                                "message": "Error occurred at the OAuth process. Please check your Access Token to fix this. Error Message: invalid_request",
                                "user_message": "",
                                "reason": "",
                                "user_message_details": {}
                            }}
                            """.trimIndent(),
                        )
                        .build()
                    "/web" -> webResponse
                    "/login" -> loginResponse
                    "/image.png" -> imageResponse
                    else -> MockResponse(code = 404)
                }
            }
            start(InetAddress.getByName("127.0.0.1"), 0)
        }
        val port: Int
            get() = server.port
        val requestCount: Int
            get() = server.requestCount

        fun diagnostics(
            loginHost: String = "localhost",
            appApi: String = "https://localhost:$port/api",
        ) = NetworkDiagnostics(
            endpoints = NetworkDiagnosticEndpoints(
                appApi = appApi,
                web = "https://localhost:$port/web",
                login = "https://$loginHost:$port/login",
                image = "https://i.pximg.net:$port/image.png",
            ),
            clientFactory = { settings, image, trace ->
                createOkHttpDiagnosticClient(settings, image, trace) {
                    sslSocketFactory(trusted.sslSocketFactory(), trusted.trustManager)
                }
            },
        )

        fun requests(count: Int) = List(count) {
            assertNotNull(server.takeRequest(1, TimeUnit.SECONDS))
        }

        override fun close() = server.close()
    }

    private companion object {
        fun html(body: String): MockResponse = MockResponse.Builder()
            .addHeader("Content-Type", "text/html; charset=utf-8")
            .body(body)
            .build()

        fun redirect(location: String): MockResponse = MockResponse.Builder()
            .code(302)
            .addHeader("Location", location)
            .build()
    }
}
