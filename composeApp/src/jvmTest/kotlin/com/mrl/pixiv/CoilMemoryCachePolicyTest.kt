package com.mrl.pixiv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoilMemoryCachePolicyTest {
    private val mib = 1024L * 1024L

    @Test
    fun capsMainAppMemoryCacheToSafeJetsamHeadroom() {
        assertEquals(64L * mib, calculateIosMemoryCacheBytes(availableMemoryBytes = null, isAppExtension = false))
        assertEquals(64L * mib, calculateIosMemoryCacheBytes(availableMemoryBytes = 0L, isAppExtension = false))
        assertEquals(64L * mib, calculateIosMemoryCacheBytes(availableMemoryBytes = 2048L * mib, isAppExtension = false))
        assertEquals(30L * mib, calculateIosMemoryCacheBytes(availableMemoryBytes = 300L * mib, isAppExtension = false))
        assertEquals(16L * mib, calculateIosMemoryCacheBytes(availableMemoryBytes = 100L * mib, isAppExtension = false))
    }

    @Test
    fun appliesStricterLimitInsideIosAppExtensions() {
        assertEquals(24L * mib, calculateIosMemoryCacheBytes(availableMemoryBytes = null, isAppExtension = true))
        assertEquals(24L * mib, calculateIosMemoryCacheBytes(availableMemoryBytes = 512L * mib, isAppExtension = true))
        assertEquals(12L * mib, calculateIosMemoryCacheBytes(availableMemoryBytes = 120L * mib, isAppExtension = true))
        assertEquals(8L * mib, calculateIosMemoryCacheBytes(availableMemoryBytes = 50L * mib, isAppExtension = true))
    }

    @Test
    fun detectsAppExtensionFromRealMainExecutableEvenWhenMainBundleIsRewritten() {
        val guestBundle = "/private/var/mobile/Containers/Data/Application/UUID/Documents/Applications/com.mrl.pixiv.PiPixiv.app"
        val hostApp = "/private/var/containers/Bundle/Application/UUID/LiveContainer.app"

        assertTrue(isIosAppExtensionProcess(guestBundle, "$hostApp/PlugIns/LiveProcess.appex/LiveProcess"))
        assertTrue(isIosAppExtensionProcess("$hostApp/PlugIns/LiveProcess.appex", null))
        assertFalse(isIosAppExtensionProcess(guestBundle, "$hostApp/LiveContainer"))
        assertFalse(
            isIosAppExtensionProcess(
                "/var/containers/Bundle/Application/UUID/PiPixiv.app",
                "/var/containers/Bundle/Application/UUID/PiPixiv.app/PiPixiv",
            ),
        )
    }
}
