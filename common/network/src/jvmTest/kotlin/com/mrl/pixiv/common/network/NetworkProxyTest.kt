package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Dns
import okhttp3.OkHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NetworkProxyTest {
    private val clientFactories: List<(BypassSetting) -> HttpClient> = listOf(
        { setting -> httpClient(OkHttp) { configureProxyOrSNI(setting) } },
        { setting -> imageHttpClient(OkHttp) { configureProxyOrSNI(setting) } },
        { setting -> HttpClient(OkHttp) { configureNetworkProxy(setting) } },
    )

    @Test
    fun systemModeUsesProxyForApiImageAndAuxiliaryRequests() = runBlocking {
        MockWebServer().use { proxy ->
            proxy.start()
            withSystemProxy({ listOf(proxy.httpProxy()) }) {
                for (factory in clientFactories) {
                    proxy.enqueue(MockResponse(body = "proxied"))
                    factory(BypassSetting.System).use { client ->
                        // 此域名不可解析，只有请求实际送到代理才会成功。
                        assertEquals("proxied", client.get("http://proxy-target.invalid/resource").bodyAsText())
                    }
                    val request = checkNotNull(proxy.takeRequest(2, TimeUnit.SECONDS))
                    assertTrue(request.requestLine.startsWith("GET http://proxy-target.invalid/resource"))
                }
            }
        }
    }

    @Test
    fun systemModeConnectsDirectlyWhenSelectorHasNoProxy() = runBlocking {
        MockWebServer().use { origin ->
            origin.start()
            withSystemProxy({ listOf(Proxy.NO_PROXY) }) {
                for (factory in clientFactories) {
                    origin.enqueue(MockResponse(body = "direct"))
                    factory(BypassSetting.System).use { client ->
                        assertEquals("direct", client.get(origin.url("/").toString()).bodyAsText())
                    }
                }
                assertEquals(clientFactories.size, origin.requestCount)
            }
        }
    }

    @Test
    fun systemModeHonorsSelectorBypassRules() = runBlocking {
        MockWebServer().use { proxy ->
            MockWebServer().use { origin ->
                proxy.start()
                origin.start()
                withSystemProxy({ uri ->
                    if (uri.host == "localhost") listOf(Proxy.NO_PROXY) else listOf(proxy.httpProxy())
                }) {
                    origin.enqueue(MockResponse(body = "bypassed"))
                    proxy.enqueue(MockResponse(body = "proxied"))
                    HttpClient(OkHttp) { configureNetworkProxy(BypassSetting.System) }.use { client ->
                        val localUrl = origin.url("/").newBuilder().host("localhost").build().toString()
                        assertEquals("bypassed", client.get(localUrl).bodyAsText())
                        assertEquals("proxied", client.get("http://proxy-target.invalid/").bodyAsText())
                    }
                    assertEquals(1, origin.requestCount)
                    assertEquals(1, proxy.requestCount)
                }
            }
        }
    }

    @Test
    fun directModeIgnoresConfiguredSystemProxyForEveryClient() = runBlocking {
        MockWebServer().use { proxy ->
            MockWebServer().use { origin ->
                proxy.start()
                origin.start()
                withSystemProxy({ error("直连模式不应查询系统代理") }) {
                    for (factory in clientFactories) {
                        origin.enqueue(MockResponse(body = "direct"))
                        factory(BypassSetting.Direct).use { client ->
                            assertEquals("direct", client.get(origin.url("/").toString()).bodyAsText())
                        }
                    }
                }
                assertEquals(0, proxy.requestCount)
            }
        }
    }

    @Test
    fun manualHttpProxyOverridesSystemProxyForEveryClient() = runBlocking {
        MockWebServer().use { manual ->
            manual.start()
            withSystemProxy({ error("手动代理不应查询系统代理") }) {
                for (factory in clientFactories) {
                    manual.enqueue(MockResponse(body = "manual"))
                    factory(BypassSetting.Proxy(manual.hostName, manual.port)).use { client ->
                        assertEquals("manual", client.get("http://proxy-target.invalid/").bodyAsText())
                    }
                }
                assertEquals(clientFactories.size, manual.requestCount)
            }
        }
    }

    @Test
    fun manualSocksProxyKeepsProtocolAndUnresolvedAddress() {
        val client = OkHttpClient.Builder().configureNetworkRouting(
            BypassSetting.Proxy("socks.example", 1081, BypassSetting.Proxy.ProxyType.SOCKS),
            usePixivSni = true,
        ).build()
        val proxy = checkNotNull(client.proxy)
        val address = assertIs<InetSocketAddress>(proxy.address())
        assertEquals(Proxy.Type.SOCKS, proxy.type())
        assertEquals("socks.example", address.hostString)
        assertEquals(1081, address.port)
        assertTrue(address.isUnresolved)
    }

    @Test
    fun returningToSystemClearsExplicitProxy() {
        val client = OkHttpClient.Builder()
            .configureNetworkRouting(BypassSetting.Direct, usePixivSni = false)
            .configureNetworkRouting(BypassSetting.System, usePixivSni = false)
            .build()
        assertNull(client.proxy)
    }

    @Test
    fun sniAppliesOnlyToPixivClientsAndNeverUsesSystemProxy() {
        val pixiv = OkHttpClient.Builder().configureNetworkRouting(BypassSetting.SNI(), true).build()
        val auxiliary = OkHttpClient.Builder().configureNetworkRouting(BypassSetting.SNI(), false).build()
        assertEquals(Proxy.NO_PROXY, pixiv.proxy)
        assertIs<SniReplaceDns>(pixiv.dns)
        assertEquals(Proxy.NO_PROXY, auxiliary.proxy)
        assertSame(Dns.SYSTEM, auxiliary.dns)
    }

    @Test
    fun sniDnsQueriesDoNotInheritSystemProxy() {
        MockWebServer().use { origin ->
            origin.start()
            origin.enqueue(MockResponse(body = """{"Status":0,"Answer":[{"type":1,"data":"203.0.113.7"}]}"""))
            withSystemProxy({ error("SNI 的 DoH 查询不应经过系统代理") }) {
                val resolver = DnsJsonResolver(origin.url("/dns-query").toString(), 2, false)
                assertEquals("203.0.113.7", resolver.lookup("example.test").single().hostAddress)
                assertEquals(1, origin.requestCount)
            }
        }
    }

    private fun MockWebServer.httpProxy() =
        Proxy(Proxy.Type.HTTP, InetSocketAddress(hostName, port))

    private inline fun <T> withSystemProxy(
        crossinline selectProxies: (URI) -> List<Proxy>,
        block: () -> T,
    ): T {
        val previous = ProxySelector.getDefault()
        ProxySelector.setDefault(object : ProxySelector() {
            override fun select(uri: URI): List<Proxy> = selectProxies(uri)
            override fun connectFailed(uri: URI, address: SocketAddress, exception: IOException) = Unit
        })
        return try {
            block()
        } finally {
            ProxySelector.setDefault(previous)
        }
    }
}
