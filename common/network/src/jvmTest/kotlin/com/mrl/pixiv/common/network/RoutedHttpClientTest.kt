package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class RoutedHttpClientTest {
    @Test
    fun newRequestsReachNewProxyWhileExistingResponseKeepsItsClient() = runBlocking {
        MockWebServer().use { proxyA ->
            MockWebServer().use { proxyB ->
                proxyA.start()
                proxyB.start()
                val routeA = BypassSetting.Proxy(proxyA.hostName, proxyA.port)
                val routeB = BypassSetting.Proxy(proxyB.hostName, proxyB.port)
                var preference = UserPreference(bypassSetting = routeA)
                val created = mutableListOf<HttpClient>()
                val closed = mutableListOf<HttpClient>()
                val clients = RoutedHttpClient(
                    readSetting = { preference.bypassSetting },
                    create = { setting ->
                        HttpClient(OkHttp) { configureNetworkProxy(setting) }.also(created::add)
                    },
                    close = { client ->
                        closed += client
                        client.close()
                    },
                    readPreference = { preference.networkSettings() },
                )

                try {
                    withTimeout(10.seconds) {
                        proxyA.enqueue(
                            MockResponse.Builder().body("old body")
                                .throttleBody(1, 100, TimeUnit.MILLISECONDS)
                                .build(),
                        )
                        proxyA.enqueue(MockResponse(body = "nested"))
                        proxyB.enqueue(MockResponse(body = "new body"))
                        proxyB.enqueue(MockResponse(body = "still new"))
                        val responseStarted = CompletableDeferred<Unit>()
                        val readBody = CompletableDeferred<Unit>()
                        val old = async {
                            clients.withClient { client ->
                                client.prepareGet("$ORIGIN/old").execute { response ->
                                    responseStarted.complete(Unit)
                                    readBody.await()
                                    val body = response.bodyAsText()
                                    // An OAuth call triggered within an older operation keeps that operation's route.
                                    val nested = clients.withClient { it.get("$ORIGIN/nested").bodyAsText() }
                                    body to nested
                                }
                            }
                        }

                        responseStarted.await()
                        preference = preference.copy(bypassSetting = routeB)
                        assertEquals("new body", clients.withClient { it.get("$ORIGIN/new").bodyAsText() })
                        assertTrue(closed.isEmpty())
                        readBody.complete(Unit)
                        assertEquals("old body" to "nested", old.await())
                        assertEquals(listOf(created.first()), closed)
                        assertEquals("still new", clients.withClient { it.get("$ORIGIN/again").bodyAsText() })
                        assertEquals(2, created.size)
                        assertEquals(listOf("/old", "/nested"), proxyA.requestPaths(2))
                        assertEquals(listOf("/new", "/again"), proxyB.requestPaths(2))
                        assertEquals(2, proxyA.requestCount)
                        assertEquals(2, proxyB.requestCount)
                    }
                } finally {
                    created.forEach(HttpClient::close)
                }
            }
        }
    }

    @Test
    fun cancellationReleasesRetiredClientAndEqualConfigurationReusesCurrentClient() = runBlocking {
        withTimeout(5.seconds) {
            var setting: BypassSetting = BypassSetting.System
            var next = 0
            val closed = mutableListOf<Int>()
            val clients = RoutedHttpClient({ setting }, { next++ }, { closed += it })
            val started = CompletableDeferred<Unit>()
            val hold = CompletableDeferred<Unit>()
            val old = async {
                clients.withClient {
                    started.complete(Unit)
                    hold.await()
                }
            }
            started.await()

            setting = BypassSetting.Direct
            assertEquals(1, clients.withClient { it })
            assertEquals(1, clients.withClient { it })
            assertTrue(closed.isEmpty())
            old.cancelAndJoin()
            assertEquals(listOf(0), closed)
        }
    }

    @Test
    fun imageSourceChangesOnlyTheCanonicalImageHostAndKeepsEncodedUrl() {
        assertEquals(
            "https://i.pixiv.cat/image%20file.jpg?version=%2B",
            resolveImageUrl("https://i.pximg.net/image%20file.jpg?version=%2B", "i.pixiv.cat"),
        )
        assertEquals(
            "https://s.pximg.net/stamp.png",
            resolveImageUrl("https://s.pximg.net/stamp.png", "i.pixiv.cat"),
        )
    }

    private fun MockWebServer.requestPaths(count: Int): List<String> = List(count) {
        val request = checkNotNull(takeRequest(2, TimeUnit.SECONDS))
        request.requestLine.removePrefix("GET $ORIGIN").substringBefore(' ')
    }

    private companion object {
        const val ORIGIN = "http://proxy-target.invalid"
    }
}
