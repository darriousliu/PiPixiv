package com.mrl.pixiv.login

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val WEBVIEW_RUNTIME_KEY =
    "SOFTWARE\\Microsoft\\EdgeUpdate\\Clients\\{F3017226-FE2A-4295-8BDF-00C3A9A7E4C5}"

internal data class WebViewRuntimeRegistryLocation(val key: String, val view: Int)

internal val webViewRuntimeRegistryLocations = listOf(
    WebViewRuntimeRegistryLocation("HKEY_LOCAL_MACHINE\\$WEBVIEW_RUNTIME_KEY", 32),
    WebViewRuntimeRegistryLocation("HKEY_CURRENT_USER\\$WEBVIEW_RUNTIME_KEY", 32),
    WebViewRuntimeRegistryLocation("HKEY_LOCAL_MACHINE\\$WEBVIEW_RUNTIME_KEY", 64),
    WebViewRuntimeRegistryLocation("HKEY_CURRENT_USER\\$WEBVIEW_RUNTIME_KEY", 64),
)

internal actual suspend fun checkWebLoginAvailability(): WebLoginAvailability =
    if (!System.getProperty("os.name").startsWith("Windows")) {
        WebLoginAvailability.Available
    } else {
        runInterruptible(Dispatchers.IO) {
            detectWindowsWebViewRuntime(::readWebViewRuntimeRegistry)
        }
    }

/** Microsoft's Evergreen Runtime registration, including per-user installs and both registry views. */
internal fun detectWindowsWebViewRuntime(
    readVersion: (WebViewRuntimeRegistryLocation) -> String?,
): WebLoginAvailability =
    if (webViewRuntimeRegistryLocations.any { location ->
            readVersion(location)?.let(::isInstalledWebViewRuntimeVersion) == true
        }
    ) {
        WebLoginAvailability.Available
    } else {
        WebLoginAvailability.Unavailable
    }

internal fun isInstalledWebViewRuntimeVersion(version: String): Boolean {
    val parts = version.trim().split('.')
    if (parts.size != 4) return false
    val numbers = parts.map { it.toLongOrNull() ?: return false }
    return numbers.all { it >= 0 } && numbers.any { it > 0 }
}

private fun readWebViewRuntimeRegistry(location: WebViewRuntimeRegistryLocation): String? {
    val process = try {
        ProcessBuilder("reg.exe", "query", location.key, "/v", "pv", "/reg:${location.view}")
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    } catch (_: IOException) {
        return null
    } catch (_: SecurityException) {
        return null
    }
    try {
        if (!process.waitFor(2, TimeUnit.SECONDS) || process.exitValue() != 0) return null
        return process.inputStream.bufferedReader().useLines { lines ->
            lines.map { it.trim().split(Regex("\\s+"), limit = 3) }
                .firstOrNull { it.size == 3 && it[0] == "pv" && it[1] == "REG_SZ" }
                ?.get(2)
        }
    } catch (_: IOException) {
        return null
    } finally {
        process.destroyForcibly()
    }
}
