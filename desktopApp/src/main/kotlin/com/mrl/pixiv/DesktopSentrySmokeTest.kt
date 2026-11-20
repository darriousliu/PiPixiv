package com.mrl.pixiv

import com.mrl.pixiv.common.analytics.captureDesktopSentrySmokeTest
import com.mrl.pixiv.common.util.AppUtil
import java.util.UUID

/** Runs only for the explicit CLI flag, before any application data is initialized. */
internal fun runDesktopSentryMappingSmokeTest(): String {
    val dsn = System.getenv("SENTRY_DSN")?.takeIf { it.isNotBlank() } ?: AppUtil.sentryDsn
    val release = "pipixiv@${AppUtil.versionName}"
    val environment = "desktop-mapping-smoke-test"
    val marker = "desktop-mapping-${UUID.randomUUID()}"
    val exception = try {
        throwDesktopSentryMappingSmokeTest(marker)
    } catch (exception: DesktopSentrySmokeTestException) {
        exception
    }

    println("SENTRY_SMOKE_TEST marker=$marker release=$release environment=$environment")
    val eventId = captureDesktopSentrySmokeTest(
        dsn = dsn,
        appVersion = AppUtil.versionName,
        appBuild = AppUtil.versionCode,
        environment = environment,
        marker = marker,
        exception = exception,
    )
    println("SENTRY_SMOKE_TEST event_id=$eventId marker=$marker flush_requested=true")
    return eventId
}

private fun throwDesktopSentryMappingSmokeTest(marker: String): Nothing {
    throw DesktopSentrySmokeTestException("Intentional desktop Sentry mapping smoke test: $marker")
}

private class DesktopSentrySmokeTestException(message: String) : IllegalStateException(message)
