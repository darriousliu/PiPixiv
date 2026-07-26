package com.mrl.pixiv.common.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiLocalNetworkAccessGateTest {
    @Test
    fun localEndpointWaitsForGrantedPermission() {
        val endpoint = "http://192.168.1.20:11434"

        assertFalse(
            canAccessAiEndpoint(endpoint, AiLocalNetworkAccessState.UNDETERMINED)
        )
        assertFalse(
            canAccessAiEndpoint(endpoint, AiLocalNetworkAccessState.DENIED)
        )
        assertTrue(
            canAccessAiEndpoint(endpoint, AiLocalNetworkAccessState.GRANTED)
        )
    }

    @Test
    fun publicHttpsDoesNotWaitForLocalNetworkPermission() {
        assertTrue(
            canAccessAiEndpoint(
                endpoint = "https://api.openai.com",
                state = AiLocalNetworkAccessState.DENIED,
            )
        )
    }

    @Test
    fun deniedPermissionIsNotAutomaticallyRequestedAgain() {
        assertEquals(
            AiLocalNetworkAccessState.DENIED,
            nextAiLocalNetworkAccessState(
                current = AiLocalNetworkAccessState.DENIED,
                retryDenied = false,
            )
        )
        assertEquals(
            AiLocalNetworkAccessState.REQUESTED,
            nextAiLocalNetworkAccessState(
                current = AiLocalNetworkAccessState.DENIED,
                retryDenied = true,
            )
        )
    }
}
