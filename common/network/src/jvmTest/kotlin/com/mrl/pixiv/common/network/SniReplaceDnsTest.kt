package com.mrl.pixiv.common.network

import okhttp3.Dns
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SniReplaceDnsTest {
    @Test
    fun configuredFallbackIsTriedBeforeDohAndSystemDns() {
        val fallbackAddress = ipv4("210.140.139.155")
        val dohAddress = ipv4("172.64.145.76")
        val systemAddress = ipv4("199.59.148.222")
        var dohHostname: String? = null
        var fallbackValue: String? = null

        val dns = SniReplaceDns(
            fallback = mapOf(API_HOST to "configured-ip"),
            dohLookup = { hostname ->
                dohHostname = hostname
                listOf(dohAddress, fallbackAddress)
            },
            systemDns = Dns {
                listOf(systemAddress, fallbackAddress)
            },
            fallbackLookup = { value ->
                fallbackValue = value
                listOf(fallbackAddress)
            },
        )

        assertEquals(
            listOf(fallbackAddress, dohAddress, systemAddress),
            dns.lookup(API_HOST),
        )
        assertEquals(API_HOST, dohHostname)
        assertEquals("configured-ip", fallbackValue)
    }

    @Test
    fun systemDnsFailureDoesNotDiscardFallback() {
        val fallbackAddress = ipv4("210.140.139.155")
        val dns = SniReplaceDns(
            fallback = mapOf(API_HOST to "configured-ip"),
            dohLookup = { throw IOException("DoH unavailable") },
            systemDns = Dns { throw UnknownHostException(it) },
            fallbackLookup = { listOf(fallbackAddress) },
        )

        assertEquals(listOf(fallbackAddress), dns.lookup(API_HOST))
    }

    @Test
    fun failedFallbackDoesNotDiscardOtherSources() {
        val dohAddress = ipv4("210.140.139.133")
        val systemAddress = ipv4("210.140.139.134")
        val dns = SniReplaceDns(
            fallback = mapOf(IMAGE_HOST to "invalid"),
            dohLookup = { listOf(dohAddress) },
            systemDns = Dns { listOf(systemAddress) },
            fallbackLookup = { throw UnknownHostException(it) },
        )

        assertEquals(
            listOf(dohAddress, systemAddress),
            dns.lookup(IMAGE_HOST),
        )
    }

    @Test
    fun emptySourcesThrowUnknownHostException() {
        val dns = SniReplaceDns(
            fallback = emptyMap(),
            dohLookup = { emptyList() },
            systemDns = Dns { emptyList() },
        )

        assertFailsWith<UnknownHostException> {
            dns.lookup("unresolvable.example")
        }
    }

    @Test
    fun invalidDohConfigurationStillAllowsFallback() {
        val fallbackAddress = ipv4("210.140.139.155")
        val resolver = DnsJsonResolver(
            queryUrl = "not a URL",
            timeoutSeconds = -1,
            unsafeSSL = false,
        )
        val dns = SniReplaceDns(
            fallback = mapOf(API_HOST to "configured-ip"),
            dohLookup = resolver::lookup,
            systemDns = Dns { throw UnknownHostException(it) },
            fallbackLookup = { listOf(fallbackAddress) },
        )

        assertEquals(listOf(fallbackAddress), dns.lookup(API_HOST))
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

    private fun ipv4(value: String): InetAddress =
        requireNotNull(parseIpv4Address(value))

    private companion object {
        const val API_HOST = "app-api.pixiv.net"
        const val IMAGE_HOST = "i.pximg.net"
    }
}
