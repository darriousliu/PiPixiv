package com.mrl.pixiv.login

import dev.nucleusframework.webview.web.NativeWebView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LoginWebViewFailureTest {
    @Test
    fun missingRuntimeDoesNotCreateNativeView() {
        var created = false
        val view = createLoginNativeWebView(WebLoginAvailability.Unavailable) {
            created = true
            error("Native factory must not run without WebView2 Runtime")
        }

        assertFalse(created)
        assertIs<WebViewRuntimeUnavailableException>(assertIs<FailedLoginNativeWebView>(view).failure)
    }

    @Test
    fun nativeCreationFailuresBecomeRecoverable() {
        val failures = listOf(
            IllegalArgumentException("Failed to create WebView2"),
            UnsatisfiedLinkError("WebView2Loader dependency could not be loaded"),
        )
        failures.forEach { failure ->
            val view = createLoginNativeWebView(WebLoginAvailability.Available) { throw failure }

            assertSame(failure, assertIs<FailedLoginNativeWebView>(view).failure)
        }
    }

    @Test
    fun successfulNativeViewRetainsItsIdentityForLibraryEmbeddingAndDisposal() {
        val nativeView = NativeWebView()

        val view = createLoginNativeWebView(WebLoginAvailability.Available) { nativeView }

        assertSame(nativeView, view)
    }

    @Test
    fun runtimeDetectionIncludesPerUserAndBothRegistryViews() {
        val visited = mutableListOf<WebViewRuntimeRegistryLocation>()
        val availability = detectWindowsWebViewRuntime { location ->
            visited += location
            if (location.key.startsWith("HKEY_CURRENT_USER") && location.view == 64) {
                "142.0.3595.69"
            } else {
                "0.0.0.0"
            }
        }

        assertEquals(WebLoginAvailability.Available, availability)
        assertEquals(setOf(32, 64), visited.map { it.view }.toSet())
        assertTrue(visited.any { it.key.startsWith("HKEY_LOCAL_MACHINE") })
    }

    @Test
    fun absentOrUninstalledRuntimeIsUnavailable() {
        assertEquals(WebLoginAvailability.Unavailable, detectWindowsWebViewRuntime { null })
        assertEquals(WebLoginAvailability.Unavailable, detectWindowsWebViewRuntime { "0.0.0.0" })
    }
}
