package com.mrl.pixiv.setting.network

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalNetworkHostTest {
    @Test
    fun loopbackHostsDoNotRequirePermission() {
        listOf("127.0.0.1", "127.1.2.3", "localhost", "LOCALHOST", "::1", "[::1]").forEach { host ->
            assertFalse(host.requiresLocalNetworkPermission(), host)
        }
    }

    @Test
    fun privateAndLinkLocalHostsRequirePermission() {
        listOf(
            "10.0.0.2",
            "172.16.0.1",
            "172.31.255.254",
            "192.168.1.10",
            " 192.168.1.10 ",
            "169.254.1.1",
            "nas.local",
        ).forEach { host ->
            assertTrue(host.requiresLocalNetworkPermission(), host)
        }
    }

    @Test
    fun publicAndInvalidHostsDoNotRequirePermission() {
        listOf(
            "8.8.8.8",
            "172.15.0.1",
            "172.32.0.1",
            "192.169.0.1",
            "10.0.0.256",
            "app-api.pixiv.net",
            "",
        ).forEach { host ->
            assertFalse(host.requiresLocalNetworkPermission(), host)
        }
    }
}
