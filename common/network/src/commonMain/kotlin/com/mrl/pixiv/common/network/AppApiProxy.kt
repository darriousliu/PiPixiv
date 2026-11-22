package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.Constants.API_HOST
import com.mrl.pixiv.common.data.Constants.AUTH_HOST
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.takeFrom

private val hostLabel = Regex("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")

fun normalizeAppApiProxyHost(input: String): String? {
    val host = input.trim().lowercase().removeSuffix(".")
    return host.takeIf { it != AUTH_HOST && it.length <= 253 && it.split('.').all(hostLabel::matches) }
}

internal fun URLBuilder.applyAppApiProxyHost(proxyHost: String) {
    if (protocol == URLProtocol.HTTPS && host.equals(API_HOST, ignoreCase = true) && proxyHost.isNotBlank()) {
        host = requireNotNull(normalizeAppApiProxyHost(proxyHost)) { "Invalid App API proxy host" }
    }
}

internal fun resolveAppApiUrl(url: String, proxyHost: String): String =
    URLBuilder().takeFrom(url).apply { applyAppApiProxyHost(proxyHost) }.buildString()
