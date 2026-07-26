package com.mrl.pixiv.common.ai

import com.mrl.pixiv.common.data.setting.AiProvider
import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiEndpointPolicyTest {
    @Test
    fun httpsEndpointsAreAllowed() {
        val result = validateAiEndpoint(" https://api.example.com/v1/ ")

        assertTrue(result.isValid)
        assertFalse(result.isLocalNetwork)
        assertEquals("https://api.example.com/v1", result.normalizedEndpoint)
    }

    @Test
    fun localHttpEndpointsAreAllowed() {
        listOf(
            "http://localhost:11434/v1",
            "http://127.0.0.1:8080",
            "http://10.0.0.2",
            "http://172.16.0.2",
            "http://172.31.255.254",
            "http://192.168.1.2",
            "http://169.254.1.2",
            "http://[::1]:8080",
            "http://[0:0:0:0:0:0:0:1]",
            "http://[fc00::1]",
            "http://[fd00::1]",
            "http://[fe80::1]",
            "http://[febf::1]",
            "http://model.local",
        ).forEach { endpoint ->
            val result = validateAiEndpoint(endpoint)
            assertTrue(result.isValid, endpoint)
            assertTrue(result.isLocalNetwork, endpoint)
        }
    }

    @Test
    fun publicHttpEndpointsAreRejected() {
        listOf(
            "http://example.com/v1",
            "http://8.8.8.8",
            "http://172.32.0.1",
            "http://192.169.0.1",
            "http://[2001:4860:4860::8888]",
            "http://[fec0::1]",
        ).forEach { endpoint ->
            val result = validateAiEndpoint(endpoint)
            assertEquals(AiEndpointError.PUBLIC_HTTP_NOT_ALLOWED, result.error, endpoint)
        }
    }

    @Test
    fun credentialsAndUnsupportedSchemesAreRejected() {
        assertEquals(
            AiEndpointError.CREDENTIALS_NOT_ALLOWED,
            validateAiEndpoint("https://user:password@example.com").error,
        )
        assertEquals(
            AiEndpointError.UNSUPPORTED_SCHEME,
            validateAiEndpoint("ftp://model.local").error,
        )
    }

    @Test
    fun emptyAndMalformedEndpointsAreRejected() {
        assertEquals(AiEndpointError.EMPTY, validateAiEndpoint(" ").error)
        assertEquals(AiEndpointError.INVALID_URL, validateAiEndpoint("model.local").error)
        assertEquals(AiEndpointError.INVALID_URL, validateAiEndpoint("http://").error)
        assertEquals(AiEndpointError.INVALID_URL, validateAiEndpoint("http://?query").error)
        assertEquals(AiEndpointError.INVALID_URL, validateAiEndpoint("http://#fragment").error)
        assertEquals(AiEndpointError.INVALID_URL, validateAiEndpoint("http://:11434").error)
        assertEquals(AiEndpointError.INVALID_URL, validateAiEndpoint("http://\\?query").error)
        assertEquals(AiEndpointError.INVALID_URL, validateAiEndpoint("http://\\evil").error)
        assertEquals(
            AiEndpointError.INVALID_URL,
            validateAiEndpoint("http://[fe80::1%25wlan0]:11434").error,
        )
        assertEquals(
            AiEndpointError.INVALID_URL,
            validateAiEndpoint("https://[fe80::1%25en0]:11434").error,
        )
    }

    @Test
    fun blankApiKeyIsAllowedOnlyForLocalOpenAiCompatibleEndpoints() {
        assertTrue(
            config(
                provider = AiProvider.OPENAI,
                endpoint = "http://model.local/v1",
                apiKey = "",
            ).isReadyForAiRequest()
        )
        assertFalse(
            config(
                provider = AiProvider.OPENAI,
                endpoint = "https://api.openai.com/v1",
                apiKey = "",
            ).isReadyForAiRequest()
        )
        assertFalse(
            config(
                provider = AiProvider.CLAUDE,
                endpoint = "http://model.local/v1",
                apiKey = "",
            ).isReadyForAiRequest()
        )
        assertTrue(
            config(
                provider = AiProvider.OPENAI,
                endpoint = "https://api.openai.com/v1",
                apiKey = "secret",
            ).isReadyForAiRequest()
        )
    }

    private fun config(
        provider: AiProvider,
        endpoint: String,
        apiKey: String,
    ) = AiTranslationConfig(
        provider = provider,
        endpoint = endpoint,
        apiKey = apiKey,
        model = "model",
    )
}
