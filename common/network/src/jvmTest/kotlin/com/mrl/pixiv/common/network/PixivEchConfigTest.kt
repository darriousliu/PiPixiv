package com.mrl.pixiv.common.network

import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PixivEchConfigTest {
    @Test
    fun dnsJsonDecodesQuotedAndUnquotedHttpsEchWithTtl() {
        for (parameter in listOf("ech=$ECH_BASE64", "ech=\\\"$ECH_BASE64\\\"")) {
            val decoded = decodePixivEchConfig(response("1 . alpn=h2 $parameter", ttl = 120))

            assertContentEquals(ECH_BYTES, decoded.bytes)
            assertEquals(120L, decoded.ttlSeconds)
        }
    }

    @Test
    fun missingOrInvalidEchFailsInsteadOfAllowingPlaintextSni() {
        assertFailsWith<IOException> {
            decodePixivEchConfig("""{"Status":0,"Authority":[{"type":6}]}""")
        }
        assertFailsWith<IOException> {
            decodePixivEchConfig(response("1 . ech=AQID"))
        }
    }

    @Test
    fun cacheRefreshesAtTtlOrInvalidationAndDoesNotReuseExpiredConfigOnFailure() {
        var nowSeconds = 0L
        var lookups = 0
        var lookupFails = false
        val cache = PixivEchConfigCache(
            lookup = {
                lookups++
                if (lookupFails) throw IOException("DNS unavailable")
                PixivEchConfig(ECH_BYTES, ttlSeconds = 10)
            },
            nanoTime = { TimeUnit.SECONDS.toNanos(nowSeconds) },
        )

        assertContentEquals(ECH_BYTES, cache.get())
        nowSeconds = 9
        cache.get()
        assertEquals(1, lookups)

        nowSeconds = 10
        cache.get()
        assertEquals(2, lookups)

        cache.invalidate()
        cache.get()
        assertEquals(3, lookups)

        lookupFails = true
        nowSeconds = 19
        assertContentEquals(ECH_BYTES, cache.get())
        nowSeconds = 20
        assertFailsWith<IOException> { cache.get() }
        assertEquals(4, lookups)
    }

    private fun response(data: String, ttl: Int = 60): String =
        """{"Status":0,"Answer":[{"type":65,"TTL":$ttl,"data":"$data"}]}"""

    private companion object {
        // cloudflare-ech.com 的公开 HTTPS 记录，仅作离线解析样例，不用于生产连接。
        const val ECH_BASE64 =
            "AEX+DQBBMAAgACCpWXjgfCszTextDRaGYJ7Pmcjt97+vFUzo9eu5OxjAFQAEAAEAAQASY2xvdWRmbGFyZS1lY2guY29tAAA="
        val ECH_BYTES: ByteArray = Base64.getDecoder().decode(ECH_BASE64)
    }
}
