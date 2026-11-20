package com.mrl.pixiv.common.analytics

import io.sentry.SentryOptions

internal fun SentryOptions.configureCrashReports(dsn: String) {
    setDsn(dsn)
    setSendDefaultPii(false)
    setEnableAutoSessionTracking(true)
    setMaxBreadcrumbs(CrashBreadcrumbLimit)
    setBeforeBreadcrumb { breadcrumb, _ ->
        stripHttpBreadcrumbQueryAndFragment(breadcrumb.category, breadcrumb.type, breadcrumb.data) { key, value ->
            if (value == null) breadcrumb.removeData(key) else breadcrumb.setData(key, value)
        }
        breadcrumb
    }
    setTracesSampleRate(null)
    setTracePropagationTargets(emptyList())
    setEnableUserInteractionTracing(false)
    logs.isEnabled = false
}
