package com.mrl.pixiv.common.analytics

import io.sentry.Breadcrumb
import io.sentry.ScopeType
import io.sentry.Sentry
import io.sentry.SentryOptions
import io.sentry.protocol.User

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

internal fun addJavaCrashBreadcrumb(category: String, message: String) {
    if (!Sentry.isEnabled()) return
    Sentry.configureScope(ScopeType.ISOLATION) { scope ->
        scope.addBreadcrumb(Breadcrumb(message).apply { this.category = category })
    }
}

internal fun configureCrashScope(userId: String?) {
    if (userId == null) return
    Sentry.configureScope(ScopeType.ISOLATION) { scope ->
        scope.user = User().apply { id = userId }
    }
}
