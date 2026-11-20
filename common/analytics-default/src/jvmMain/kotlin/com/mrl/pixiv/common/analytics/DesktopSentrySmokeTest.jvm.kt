package com.mrl.pixiv.common.analytics

import io.sentry.kotlin.multiplatform.Sentry

fun captureDesktopSentrySmokeTest(
    dsn: String,
    appVersion: String,
    appBuild: Int,
    environment: String,
    marker: String,
    exception: Throwable,
): String {
    require(dsn.isNotBlank()) { "Sentry DSN is not configured for the desktop mapping smoke test" }
    Sentry.initWithPlatformOptions { options ->
        options.configureDesktopCrashReports(dsn, appVersion, appBuild, cacheDir = null)
        options.environment = environment
        options.sampleRate = 1.0
    }
    try {
        check(Sentry.isEnabled()) { "Sentry is disabled for the desktop mapping smoke test" }
        val eventId = Sentry.captureException(exception) { scope ->
            scope.setTag("diagnostic", "desktop-sentry-mapping")
            scope.setTag("test_marker", marker)
        }.toString()
        check(eventId != "00000000000000000000000000000000") {
            "Sentry did not accept the desktop mapping smoke-test event"
        }
        // The KMP facade has no flush API; use only the JVM transport flush here.
        io.sentry.Sentry.flush(10_000L)
        return eventId
    } finally {
        Sentry.close()
    }
}
