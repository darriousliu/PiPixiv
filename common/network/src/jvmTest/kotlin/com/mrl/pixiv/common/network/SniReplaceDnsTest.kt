package com.mrl.pixiv.common.network

import okhttp3.Dns
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.InetAddress
import java.net.Proxy
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SniReplaceDnsTest {
    @Test
    fun bypassClientDoesNotInheritAProcessProxy() {
        val client = OkHttpClient.Builder()
            .bypassSNI(
                queryUrl = "https://doh.example/dns-query",
                nonStrictSSL = false,
                fallback = emptyMap(),
                dohTimeout = 5,
            )
            .build()

        assertEquals(Proxy.NO_PROXY, client.proxy)
    }

    @Test
    fun apiCombinesOriginDohWithOwnFallbackWithoutUsingSystemDns() {
        val firstDohAddress = ipv4("210.140.139.152")
        val secondDohAddress = ipv4("210.140.139.155")
        val fallbackAddress = ipv4("210.140.139.158")
        var dohHostname: String? = null
        var fallbackValue: String? = null
        var systemCalled = false

        val dns = SniReplaceDns(
            fallback = mapOf(API_HOST to "configured-pool"),
            originLookup = { hostname ->
                dohHostname = hostname
                DnsLookupResult(
                    source = "DoH($hostname)",
                    addresses = listOf(firstDohAddress, secondDohAddress),
                )
            },
            systemDns = Dns {
                systemCalled = true
                emptyList()
            },
            fallbackLookup = { value ->
                fallbackValue = value
                listOf(secondDohAddress, fallbackAddress)
            },
        )

        assertEquals(
            listOf(firstDohAddress, secondDohAddress, fallbackAddress),
            dns.lookup(API_HOST),
        )
        assertEquals(PIXIV_ORIGIN_ALIAS, dohHostname)
        assertEquals("configured-pool", fallbackValue)
        assertFalse(systemCalled)
    }

    @Test
    fun oauthUsesOriginDohAliasAndItsOwnFallback() {
        var dohHostname: String? = null
        val dohAddress = ipv4("210.140.139.158")
        val fallbackAddress = ipv4("210.140.139.161")
        val dns = SniReplaceDns(
            fallback = mapOf(AUTH_HOST to "210.140.139.161"),
            originLookup = {
                dohHostname = it
                DnsLookupResult("DoH($it)", listOf(dohAddress))
            },
            systemDns = Dns { error("System DNS must not be used") },
        )

        assertEquals(listOf(dohAddress, fallbackAddress), dns.lookup(AUTH_HOST))
        assertEquals(PIXIV_ORIGIN_ALIAS, dohHostname)
    }

    @Test
    fun imageHostsUseOriginDohAliasesBeforeTheirFallback() {
        val queriedHosts = mutableListOf<String>()
        val dohAddress = ipv4("210.140.139.133")
        val fallbackAddress = ipv4("210.140.139.134")
        val dns = SniReplaceDns(
            fallback = mapOf(
                IMAGE_HOST to "210.140.139.134",
                STATIC_IMAGE_HOST to "210.140.139.134",
            ),
            originLookup = {
                queriedHosts += it
                DnsLookupResult("DoH($it)", listOf(dohAddress))
            },
            systemDns = Dns { error("System DNS must not be used") },
        )

        assertEquals(listOf(dohAddress, fallbackAddress), dns.lookup(IMAGE_HOST))
        assertEquals(listOf(dohAddress, fallbackAddress), dns.lookup(STATIC_IMAGE_HOST))
        assertEquals(
            listOf(
                "$IMAGE_HOST.cdn.cloudflare.net",
                "$STATIC_IMAGE_HOST.cdn.cloudflare.net",
            ),
            queriedHosts,
        )
    }

    @Test
    fun dohFailureFallsBackWithoutCallingSystemDns() {
        val fallbackAddress = ipv4("210.140.139.155")
        var systemCalled = false
        val dns = SniReplaceDns(
            fallback = mapOf(API_HOST to "configured-ip"),
            originLookup = { throw IOException("Origin DNS unavailable") },
            systemDns = Dns {
                systemCalled = true
                throw UnknownHostException(it)
            },
            fallbackLookup = { listOf(fallbackAddress) },
        )

        assertEquals(listOf(fallbackAddress), dns.lookup(API_HOST))
        assertFalse(systemCalled)
    }

    @Test
    fun unknownHostUsesOnlySystemDns() {
        val systemAddress = ipv4("192.0.2.10")
        var originCalled = false
        var fallbackCalled = false
        val dns = SniReplaceDns(
            fallback = emptyMap(),
            originLookup = {
                originCalled = true
                null
            },
            systemDns = Dns { listOf(systemAddress) },
            fallbackLookup = {
                fallbackCalled = true
                emptyList()
            },
        )

        assertEquals(listOf(systemAddress), dns.lookup("example.com"))
        assertFalse(originCalled)
        assertFalse(fallbackCalled)
    }

    @Test
    fun managedHostDoesNotFallThroughToSystemDns() {
        var systemCalled = false
        val dns = SniReplaceDns(
            fallback = emptyMap(),
            originLookup = { null },
            systemDns = Dns {
                systemCalled = true
                listOf(ipv4("104.18.42.239"))
            },
        )

        assertFailsWith<UnknownHostException> {
            dns.lookup(API_HOST)
        }
        assertFalse(systemCalled)
    }

    @Test
    fun fallbackFailureKeepsDohAddressesWithoutCallingSystemDns() {
        val dohAddress = ipv4("210.140.139.152")
        var systemCalled = false
        val dns = SniReplaceDns(
            fallback = mapOf(API_HOST to "invalid fallback"),
            originLookup = {
                DnsLookupResult("DoH($it)", listOf(dohAddress))
            },
            systemDns = Dns {
                systemCalled = true
                emptyList()
            },
            fallbackLookup = { throw IOException("fallback unavailable") },
        )

        assertEquals(listOf(dohAddress), dns.lookup(API_HOST))
        assertFalse(systemCalled)
    }

    @Test
    fun invalidDohConfigurationStillAllowsFallback() {
        val fallbackAddress = ipv4("210.140.139.155")
        val resolver = DnsJsonResolver(
            queryUrl = "not a URL",
            timeoutSeconds = -1,
            unsafeSSL = false,
        )
        val originResolver = RacingOriginDnsResolver(
            timeoutSeconds = 1,
            dohLookup = resolver::lookup,
            systemLookup = { throw UnknownHostException(it) },
        )
        val dns = SniReplaceDns(
            fallback = mapOf(API_HOST to "configured-ip"),
            originLookup = originResolver::lookup,
            systemDns = Dns { throw UnknownHostException(it) },
            fallbackLookup = { listOf(fallbackAddress) },
        )

        assertEquals(listOf(fallbackAddress), dns.lookup(API_HOST))
    }

    @Test
    fun racingOriginResolverWaitsForANonEmptyDynamicResult() {
        val dohAddress = ipv4("210.140.139.152")
        var dohHostname: String? = null
        val resolver = RacingOriginDnsResolver(
            timeoutSeconds = 1,
            dohLookup = {
                dohHostname = it
                listOf(dohAddress)
            },
            systemLookup = { emptyList() },
        )

        assertEquals(
            DnsLookupResult("DoH($PIXIV_ORIGIN_ALIAS)", listOf(dohAddress)),
            resolver.lookup(PIXIV_ORIGIN_ALIAS),
        )
        assertEquals(PIXIV_ORIGIN_ALIAS, dohHostname)
    }

    @Test
    fun racingOriginResolverUsesSystemAliasWhenDohFails() {
        val systemAddress = ipv4("210.140.139.155")
        val resolver = RacingOriginDnsResolver(
            timeoutSeconds = 1,
            dohLookup = { throw IOException("DoH unavailable") },
            systemLookup = { listOf(systemAddress) },
        )

        assertEquals(
            DnsLookupResult(
                "system origin DNS($PIXIV_ORIGIN_ALIAS)",
                listOf(systemAddress),
            ),
            resolver.lookup(PIXIV_ORIGIN_ALIAS),
        )
    }

    @Test
    fun interruptedOriginLookupStopsWithoutUsingFallback() {
        val lookupStarted = CountDownLatch(1)
        val releaseLookup = CountDownLatch(1)
        val fallbackCalled = AtomicBoolean(false)
        val failure = AtomicReference<Throwable>()
        val executor = Executors.newFixedThreadPool(2)
        val resolver = RacingOriginDnsResolver(
            timeoutSeconds = 10,
            dohLookup = {
                lookupStarted.countDown()
                releaseLookup.await()
                emptyList()
            },
            systemLookup = {
                lookupStarted.countDown()
                releaseLookup.await()
                emptyList()
            },
            executor = executor,
        )
        val dns = SniReplaceDns(
            fallback = mapOf(API_HOST to "210.140.139.155"),
            originLookup = resolver::lookup,
            fallbackLookup = {
                fallbackCalled.set(true)
                listOf(ipv4(it))
            },
        )
        val lookupThread = Thread {
            try {
                dns.lookup(API_HOST)
            } catch (throwable: Throwable) {
                failure.set(throwable)
            }
        }

        try {
            lookupThread.start()
            assertTrue(lookupStarted.await(2, TimeUnit.SECONDS))
            lookupThread.interrupt()
            lookupThread.join(TimeUnit.SECONDS.toMillis(2))

            assertFalse(lookupThread.isAlive)
            assertIs<UnknownHostException>(failure.get())
            assertFalse(fallbackCalled.get())
        } finally {
            releaseLookup.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun resultsAreDeduplicatedInDohThenFallbackOrder() {
        val first = ipv4("210.140.139.161")
        val second = ipv4("210.140.139.152")
        val third = ipv4("210.140.139.155")
        val dns = SniReplaceDns(
            fallback = mapOf(API_HOST to "configured-pool"),
            originLookup = {
                DnsLookupResult("DoH($it)", listOf(first, second, first))
            },
            systemDns = Dns { error("System DNS must not be used") },
            fallbackLookup = { listOf(second, third, first) },
        )

        assertEquals(listOf(first, second, third), dns.lookup(API_HOST))
    }

    @Test
    fun dnsJsonAcceptsOnlyValidARecords() {
        val addresses = decodeDnsJsonAddresses(
            """
            {
              "Status": 0,
              "Answer": [
                {"name":"example.com.","type":5,"TTL":60,"data":"alias.example.com."},
                {"name":"example.com.","type":1,"TTL":60,"data":"210.140.139.155"},
                {"name":"example.com.","type":28,"TTL":60,"data":"2001:db8::1"},
                {"name":"example.com.","type":1,"TTL":60,"data":"999.1.1.1"},
                {"name":"example.com.","type":1,"TTL":60,"data":"210.140.139.155"}
              ]
            }
            """.trimIndent()
        )

        assertEquals(listOf(ipv4("210.140.139.155")), addresses)
    }

    @Test
    fun dnsJsonAllowsEmptyAnswerButRejectsErrorStatus() {
        assertEquals(
            emptyList(),
            decodeDnsJsonAddresses("""{"Status":0}"""),
        )
        assertFailsWith<UnknownHostException> {
            decodeDnsJsonAddresses("""{"Status":3}""")
        }
    }

    @Test
    fun ipv4ParserChecksShapeAndRange() {
        assertEquals(
            ipv4("0.255.1.42"),
            parseIpv4Address("0.255.1.42"),
        )
        assertNull(parseIpv4Address("256.1.1.1"))
        assertNull(parseIpv4Address("1.2.3"))
        assertNull(parseIpv4Address("1.2.3.example"))
    }

    @Test
    fun fallbackParserAcceptsSingleOrPooledIpv4WithoutResolvingNames() {
        assertEquals(
            listOf(ipv4("210.140.139.155")),
            parseFallbackAddresses("210.140.139.155"),
        )
        assertEquals(
            listOf(
                ipv4("210.140.139.152"),
                ipv4("210.140.139.155"),
                ipv4("210.140.139.158"),
            ),
            parseFallbackAddresses(
                "210.140.139.152, 210.140.139.155\n" +
                        "invalid.example 210.140.139.158 210.140.139.152"
            ),
        )
    }

    private fun ipv4(value: String): InetAddress =
        requireNotNull(parseIpv4Address(value))

    private companion object {
        const val API_HOST = "app-api.pixiv.net"
        const val AUTH_HOST = "oauth.secure.pixiv.net"
        const val IMAGE_HOST = "i.pximg.net"
        const val STATIC_IMAGE_HOST = "s.pximg.net"
    }
}
