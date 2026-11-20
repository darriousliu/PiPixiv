package com.mrl.pixiv.common.analytics

import io.sentry.HttpBodyType
import io.sentry.KeyValueCollectionBehavior
import io.sentry.SentryOptions

internal fun SentryOptions.configureCrashReports(dsn: String) {
    setDsn(dsn)
    // Setting any category activates new SDK defaults. Preserve the legacy opt-out.
    dataCollection.apply {
        userInfo = false
        cookies = KeyValueCollectionBehavior.off()
        httpBodies = setOf(HttpBodyType.OUTGOING_REQUEST, HttpBodyType.INCOMING_RESPONSE)
        databaseQueryData = false
        filePaths = false
        graphql.document = false
        graphql.variables = false
    }
    isEnableAutoSessionTracking = true
    maxBreadcrumbs = CrashBreadcrumbLimit
    setBeforeBreadcrumb { breadcrumb, _ ->
        stripHttpBreadcrumbQueryAndFragment(breadcrumb.category, breadcrumb.type, breadcrumb.data) { key, value ->
            if (value == null) breadcrumb.removeData(key) else breadcrumb.setData(key, value)
        }
        breadcrumb
    }
    setTracesSampleRate(null)
    setTracePropagationTargets(emptyList())
    isEnableUserInteractionTracing = false
    logs.isEnabled = false
}
