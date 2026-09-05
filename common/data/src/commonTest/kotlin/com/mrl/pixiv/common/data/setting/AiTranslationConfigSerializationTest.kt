package com.mrl.pixiv.common.data.setting

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
class AiTranslationConfigSerializationTest {
    private val json = Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
    }
    private val protoBuf = ProtoBuf {
        encodeDefaults = false
    }

    @Test
    fun `old json config uses new field defaults`() {
        val decoded = json.decodeFromString<AiTranslationConfig>(
            """
                {
                  "provider": "CLAUDE",
                  "endpoint": "https://api.example.com",
                  "apiKey": "secret",
                  "model": "model",
                  "responseApi": false,
                  "extraBody": "{}"
                }
            """.trimIndent()
        )

        assertEquals(
            AiTranslationConfig.GENERATION_TIMEOUT_DEFAULT_SECONDS,
            decoded.generationTimeoutSeconds,
        )
        assertEquals(
            AiTranslationConfig.MAX_CONCURRENT_REQUESTS_DEFAULT,
            decoded.maxConcurrentRequests,
        )
    }

    @Test
    fun `old protobuf config uses new field defaults`() {
        val legacyBytes = protoBuf.encodeToByteArray(
            LegacyAiTranslationConfig.serializer(),
            LegacyAiTranslationConfig(
                provider = AiProvider.GEMINI,
                endpoint = "https://api.example.com",
                apiKey = "secret",
                model = "model",
                responseApi = false,
                extraBody = "{}",
            )
        )

        val decoded = protoBuf.decodeFromByteArray(
            AiTranslationConfig.serializer(),
            legacyBytes,
        )

        assertEquals(AiProvider.GEMINI, decoded.provider)
        assertEquals(
            AiTranslationConfig.GENERATION_TIMEOUT_DEFAULT_SECONDS,
            decoded.generationTimeoutSeconds,
        )
        assertEquals(
            AiTranslationConfig.MAX_CONCURRENT_REQUESTS_DEFAULT,
            decoded.maxConcurrentRequests,
        )
    }

    @Test
    fun `custom request settings round trip through json and protobuf`() {
        val config = AiTranslationConfig(
            generationTimeoutSeconds = 900,
            maxConcurrentRequests = 8,
        )

        assertEquals(
            config,
            json.decodeFromString<AiTranslationConfig>(
                json.encodeToString(AiTranslationConfig.serializer(), config)
            ),
        )
        assertEquals(
            config,
            protoBuf.decodeFromByteArray(
                AiTranslationConfig.serializer(),
                protoBuf.encodeToByteArray(AiTranslationConfig.serializer(), config),
            ),
        )
    }

    @Serializable
    private data class LegacyAiTranslationConfig(
        val provider: AiProvider = AiProvider.OPENAI,
        val endpoint: String = AiTranslationConfig.defaultEndpoint(AiProvider.OPENAI),
        val apiKey: String = "",
        val model: String = AiTranslationConfig.defaultModel(AiProvider.OPENAI).modelId,
        val responseApi: Boolean = false,
        val extraBody: String = "",
    )
}
