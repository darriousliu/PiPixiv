package com.mrl.pixiv.common.ai

import com.mrl.pixiv.common.data.setting.AiProvider
import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import io.ktor.http.URLProtocol
import io.ktor.http.Url

enum class AiEndpointError {
    EMPTY,
    INVALID_URL,
    UNSUPPORTED_SCHEME,
    CREDENTIALS_NOT_ALLOWED,
    PUBLIC_HTTP_NOT_ALLOWED,
}

data class AiEndpointValidation(
    val normalizedEndpoint: String? = null,
    val error: AiEndpointError? = null,
    val isLocalNetwork: Boolean = false,
) {
    val isValid: Boolean
        get() = error == null && normalizedEndpoint != null
}

fun validateAiEndpoint(endpoint: String): AiEndpointValidation {
    val normalized = endpoint.trim().trimEnd('/')
    if (normalized.isEmpty()) {
        return AiEndpointValidation(error = AiEndpointError.EMPTY)
    }
    if (!normalized.contains("://")) {
        return AiEndpointValidation(error = AiEndpointError.INVALID_URL)
    }
    if (normalized.rawAuthorityOrNull() == null) {
        return AiEndpointValidation(error = AiEndpointError.INVALID_URL)
    }

    val url = runCatching { Url(normalized) }.getOrNull()
        ?: return AiEndpointValidation(error = AiEndpointError.INVALID_URL)
    if (url.protocol != URLProtocol.HTTP && url.protocol != URLProtocol.HTTPS) {
        return AiEndpointValidation(error = AiEndpointError.UNSUPPORTED_SCHEME)
    }
    if (url.host.isBlank()) {
        return AiEndpointValidation(error = AiEndpointError.INVALID_URL)
    }
    if (url.host.isScopedIpv6Host()) {
        return AiEndpointValidation(error = AiEndpointError.INVALID_URL)
    }
    if (url.user != null || url.password != null) {
        return AiEndpointValidation(error = AiEndpointError.CREDENTIALS_NOT_ALLOWED)
    }

    val isLocalNetwork = url.host.isLocalNetworkHost()
    if (url.protocol == URLProtocol.HTTP && !isLocalNetwork) {
        return AiEndpointValidation(error = AiEndpointError.PUBLIC_HTTP_NOT_ALLOWED)
    }
    return AiEndpointValidation(
        normalizedEndpoint = normalized,
        isLocalNetwork = isLocalNetwork,
    )
}

fun AiTranslationConfig.isReadyForAiRequest(): Boolean {
    val endpointValidation = validateAiEndpoint(endpoint)
    val hasCredentials = apiKey.isNotBlank() ||
            provider == AiProvider.OPENAI && endpointValidation.isLocalNetwork
    val hasValidGenerationTimeout =
        generationTimeoutSeconds in AiTranslationConfig.GENERATION_TIMEOUT_MIN_SECONDS..
            AiTranslationConfig.GENERATION_TIMEOUT_MAX_SECONDS
    return endpointValidation.isValid &&
            model.isNotBlank() &&
            hasCredentials &&
            hasValidGenerationTimeout
}

internal fun requireValidAiEndpoint(endpoint: String): String {
    val validation = validateAiEndpoint(endpoint)
    require(validation.isValid) {
        "Invalid AI endpoint: ${validation.error}"
    }
    return requireNotNull(validation.normalizedEndpoint)
}

private fun String.isLocalNetworkHost(): Boolean {
    val normalized = trim().trim('[', ']').lowercase()
    if (
        normalized == "localhost" ||
        normalized.endsWith(".local")
    ) {
        return true
    }

    if (':' in normalized) {
        val groups = normalized.toIpv6Groups() ?: return false
        val isLoopback = groups.dropLast(1).all { it == 0 } && groups.last() == 1
        val firstGroup = groups.first()
        val isUniqueLocal = firstGroup and 0xFE00 == 0xFC00
        val isLinkLocal = firstGroup and 0xFFC0 == 0xFE80
        return isLoopback || isUniqueLocal || isLinkLocal
    }

    val octets = normalized.split('.')
        .takeIf { it.size == 4 }
        ?.map { it.toIntOrNull() ?: return false }
        ?: return false
    if (octets.any { it !in 0..255 }) return false

    return octets[0] == 10 ||
            octets[0] == 127 ||
            octets[0] == 169 && octets[1] == 254 ||
            octets[0] == 172 && octets[1] in 16..31 ||
            octets[0] == 192 && octets[1] == 168
}

private fun String.rawAuthorityOrNull(): String? {
    val schemeSeparator = indexOf("://")
    if (schemeSeparator <= 0) return null
    val authorityStart = schemeSeparator + 3
    val authorityEnd = indexOfAny(
        chars = charArrayOf('/', '\\', '?', '#'),
        startIndex = authorityStart,
    ).takeIf { it >= 0 } ?: length
    val authority = substring(authorityStart, authorityEnd)
    if (authority.isBlank() || authority.startsWith(':')) return null
    return authority
}

private fun String.isScopedIpv6Host(): Boolean {
    val normalized = trim().trim('[', ']')
    return ':' in normalized && '%' in normalized
}

private fun String.toIpv6Groups(): List<Int>? {
    if (isEmpty() || any { it != ':' && it !in '0'..'9' && it.lowercaseChar() !in 'a'..'f' }) {
        return null
    }
    if (count { it == ':' } < 2 || split("::").size > 2) return null

    val hasCompression = contains("::")
    val halves = split("::", limit = 2)
    val leading = halves.first().toIpv6Half() ?: return null
    val trailing = if (hasCompression) {
        halves.getOrElse(1) { "" }.toIpv6Half() ?: return null
    } else {
        emptyList()
    }
    if (!hasCompression) {
        return leading.takeIf { it.size == IPV6_GROUP_COUNT }
    }

    val omittedCount = IPV6_GROUP_COUNT - leading.size - trailing.size
    if (omittedCount < 1) return null
    return leading + List(omittedCount) { 0 } + trailing
}

private fun String.toIpv6Half(): List<Int>? {
    if (isEmpty()) return emptyList()
    return split(':').map { group ->
        if (group.isEmpty() || group.length > 4) return null
        group.toIntOrNull(radix = 16) ?: return null
    }
}

private const val IPV6_GROUP_COUNT = 8
