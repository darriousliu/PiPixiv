package com.mrl.pixiv.common.analytics

import io.sentry.Breadcrumb
import io.sentry.Hint
import io.sentry.SendCachedEnvelopeFireAndForgetIntegration
import io.sentry.SentryOptions
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CrashDiagnosticsTest {
    private val testDsn = "https://public@example.invalid/1"
    private val beforeBreadcrumb = assertNotNull(
        SentryOptions().apply { configureCrashReports(testDsn) }.beforeBreadcrumb,
    )

    @Test
    fun removesOnlyTheQueryAndFragmentFromHttpBreadcrumbs() {
        val url = "https://api.example.org/v1/12345_p0.jpg%3F"
        val breadcrumb = Breadcrumb("HTTP request").apply {
            category = "network.request"
            type = "http"
            setData("url", "$url?word=private#private")
            setData("http.query", "word=private")
            setData("http.fragment", "private")
            setData("method", "GET")
            setData("status_code", 404)
            setData("response_body_size", 357)
        }

        assertSame(breadcrumb, beforeBreadcrumb.execute(breadcrumb, Hint()))
        assertEquals(
            mapOf("url" to url, "method" to "GET", "status_code" to 404, "response_body_size" to 357),
            breadcrumb.data,
        )
        assertEquals("HTTP request", breadcrumb.message)
    }

    @Test
    fun leavesNonHttpBreadcrumbsUntouched() {
        val breadcrumb = Breadcrumb("Entered the page").apply {
            category = "http.navigation"
            type = "navigation"
            setData("url", "https://example.org/page?tab=latest#top")
            setData("http.query", "tab=latest")
            setData("http.fragment", "top")
            setData("visible", true)
        }
        val original = breadcrumb.data.toMap()

        assertSame(breadcrumb, beforeBreadcrumb.execute(breadcrumb, Hint()))
        assertEquals(original, breadcrumb.data)
    }

    @Test
    fun acceptsFixedIdentifiersAndRejectsRouteArguments() {
        assertTrue(isValidAppCrashBreadcrumb(AppCrashScreenCategory, "PictureDeeplink"))
        assertTrue(isValidAppCrashBreadcrumb(AppCrashTabCategory, "SearchResults/Illustrations"))
        assertTrue(isValidAppCrashBreadcrumb(AppCrashOperationCategory, "DownloadImage"))

        assertFalse(isValidAppCrashBreadcrumb(AppCrashScreenCategory, "PictureDetail/1234"))
        assertFalse(isValidAppCrashBreadcrumb(AppCrashTabCategory, "SearchResults/Illustrations?word=private"))
        assertFalse(isValidAppCrashBreadcrumb(AppCrashOperationCategory, "DownloadImage https://example.org"))
    }

    @Test
    fun keepsDesktopSessionPrivacyAndCacheOptions() {
        val cacheDir = Files.createTempDirectory("pipixiv-sentry-cache")
        try {
            val options = SentryOptions().apply {
                configureDesktopCrashReports(
                    dsn = testDsn,
                    appVersion = "2.6.1",
                    appBuild = 20601,
                    cacheDir = cacheDir.toString(),
                )
            }

            assertEquals("pipixiv@2.6.1", options.release)
            assertEquals("20601", options.dist)
            val configuredPath = Path.of(assertNotNull(options.cacheDirPath))
            assertTrue(configuredPath == cacheDir || configuredPath.parent == cacheDir)
            assertTrue(options.integrations.any { it is SendCachedEnvelopeFireAndForgetIntegration })
            assertTrue(options.isEnableAutoSessionTracking)
            assertFalse(options.isSendDefaultPii)
            assertFalse(options.isAttachServerName)
            assertNull(options.beforeSend)
        } finally {
            cacheDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun omitsCachedEnvelopeIntegrationWithoutACacheDirectory() {
        val options = SentryOptions().apply {
            configureDesktopCrashReports(testDsn, appVersion = "2.6.1", appBuild = 20601, cacheDir = null)
        }

        assertNull(options.cacheDirPath)
        assertFalse(options.integrations.any { it is SendCachedEnvelopeFireAndForgetIntegration })
    }
}
