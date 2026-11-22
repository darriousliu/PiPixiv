package com.mrl.pixiv.common.analytics

import io.sentry.KeyValueCollectionBehavior
import io.sentry.SentryOptions

internal fun SentryOptions.configureCrashReports(dsn: String) {
    setDsn(dsn)
    // Setting any category activates new SDK defaults, so each category is set explicitly.
    // HTTP bodies stay off because API responses can contain user content.
    dataCollection.apply {
        userInfo = false
        cookies = KeyValueCollectionBehavior.off()
        httpBodies = emptySet()
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
    logs.isEnabled = true
}
