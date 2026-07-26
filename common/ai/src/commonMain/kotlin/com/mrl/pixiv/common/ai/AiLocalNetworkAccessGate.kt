package com.mrl.pixiv.common.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AiLocalNetworkAccessState {
    UNDETERMINED,
    REQUESTED,
    GRANTED,
    DENIED,
}

/**
 * Coordinates Android 17's runtime local-network permission with background AI work.
 *
 * The queue asks for access before claiming a local task. The UI resolves the request after the
 * platform permission callback, so a denied or unanswered prompt never consumes a queue retry.
 */
object AiLocalNetworkAccessGate {
    private val mutableState = MutableStateFlow(AiLocalNetworkAccessState.UNDETERMINED)

    val state = mutableState.asStateFlow()

    fun requestAccess(retryDenied: Boolean = false) {
        mutableState.update { current ->
            nextAiLocalNetworkAccessState(current, retryDenied)
        }
    }

    fun resolve(granted: Boolean) {
        mutableState.value = if (granted) {
            AiLocalNetworkAccessState.GRANTED
        } else {
            AiLocalNetworkAccessState.DENIED
        }
    }

    fun canAccess(endpoint: String): Boolean =
        canAccessAiEndpoint(endpoint, mutableState.value)
}

internal fun nextAiLocalNetworkAccessState(
    current: AiLocalNetworkAccessState,
    retryDenied: Boolean,
): AiLocalNetworkAccessState = when {
    current == AiLocalNetworkAccessState.GRANTED -> current
    current == AiLocalNetworkAccessState.DENIED && !retryDenied -> current
    else -> AiLocalNetworkAccessState.REQUESTED
}

internal fun canAccessAiEndpoint(
    endpoint: String,
    state: AiLocalNetworkAccessState,
): Boolean {
    val validation = validateAiEndpoint(endpoint)
    return validation.isValid &&
            (!validation.isLocalNetwork || state == AiLocalNetworkAccessState.GRANTED)
}
