package com.mrl.pixiv.common.analytics

import io.sentry.Breadcrumb
import io.sentry.Hint
import io.sentry.HttpBodyType
import io.sentry.KeyValueCollectionBehavior
import io.sentry.SendCachedEnvelopeFireAndForgetIntegration
import io.sentry.SentryLogEvent
import io.sentry.SentryLogLevel
import io.sentry.SentryOptions
import io.sentry.protocol.SentryId
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
            assertTrue(options.logs.isEnabled)
            with(options.dataCollection) {
                assertEquals(false, userInfo)
                assertEquals(KeyValueCollectionBehavior.off(), cookies)
                assertEquals(emptySet<HttpBodyType>(), httpBodies)
                assertEquals(false, databaseQueryData)
                assertEquals(false, filePaths)
                assertEquals(false, graphql.document)
                assertEquals(false, graphql.variables)
            }
            assertFalse(options.isAttachServerName)
            assertNull(options.beforeSend)
        } finally {
            cacheDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun addsOperatingSystemToDesktopLogs() {
        val options = SentryOptions().apply {
            configureDesktopCrashReports(testDsn, appVersion = "2.6.1", appBuild = 20601, cacheDir = null)
        }
        val beforeSendLog = assertNotNull(options.logs.beforeSend)
        val log = SentryLogEvent(SentryId(), 0.0, "screen_view", SentryLogLevel.INFO)

        val attributes = assertNotNull(assertNotNull(beforeSendLog.execute(log)).attributes)
        val expectedOsName = System.getProperty("os.name").let { if (it == "Mac OS X") "macOS" else it }
        assertEquals(expectedOsName, attributes["os.name"]?.value)
        assertEquals(System.getProperty("os.version"), attributes["os.version"]?.value)
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
