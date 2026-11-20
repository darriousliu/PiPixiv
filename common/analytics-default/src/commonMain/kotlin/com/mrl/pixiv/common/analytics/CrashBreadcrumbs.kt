package com.mrl.pixiv.common.analytics

internal const val CrashBreadcrumbLimit = 100
internal const val AppCrashScreenCategory = "app.navigation"
internal const val AppCrashTabCategory = "app.navigation.tab"
internal const val AppCrashOperationCategory = "app.operation"

private val identifier = Regex("^[A-Za-z0-9_]{1,64}$")

internal fun isDiagnosticIdentifier(value: String): Boolean = identifier.matches(value)

/** Records a compile-time screen name. Route arguments must stay out of diagnostics. */
fun recordCrashScreen(screen: String) = recordAppCrashBreadcrumb(AppCrashScreenCategory, screen)

fun recordCrashTab(screen: String, tab: Enum<*>) =
    recordAppCrashBreadcrumb(AppCrashTabCategory, "$screen/${tab.name}")

/** Records a fixed app operation name. */
fun recordCrashOperation(operation: String) = recordAppCrashBreadcrumb(AppCrashOperationCategory, operation)

private fun recordAppCrashBreadcrumb(category: String, message: String) {
    if (isValidAppCrashBreadcrumb(category, message)) addPlatformCrashBreadcrumb(category, message)
}

internal fun isValidAppCrashBreadcrumb(category: String, message: String?): Boolean {
    if (message == null) return false
    return when (category) {
        AppCrashScreenCategory, AppCrashOperationCategory -> isDiagnosticIdentifier(message)
        AppCrashTabCategory -> {
            val separator = message.indexOf('/')
            separator > 0 &&
                isDiagnosticIdentifier(message.substring(0, separator)) &&
                isDiagnosticIdentifier(message.substring(separator + 1))
        }
        else -> false
    }
}

internal expect fun addPlatformCrashBreadcrumb(category: String, message: String)
