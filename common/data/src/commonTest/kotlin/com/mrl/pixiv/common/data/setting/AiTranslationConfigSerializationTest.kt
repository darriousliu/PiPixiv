package com.mrl.pixiv.common.data.setting

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AiTranslationConfigSerializationTest {
    private val json = Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
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
    fun `custom request settings round trip through json`() {
        val config = AiTranslationConfig(
            generationTimeoutSeconds = 900,
            maxConcurrentRequests = 200,
        )

        assertEquals(
            config,
            json.decodeFromString<AiTranslationConfig>(
                json.encodeToString(AiTranslationConfig.serializer(), config)
            ),
        )
    }

    @Test
    fun `catalog updates preserve explicitly saved legacy and custom model ids`() {
        val legacyJson = Json { encodeDefaults = true }
        val savedModels = listOf(
            AiProvider.OPENAI to "gpt-5.4-mini",
            AiProvider.CLAUDE to "claude-opus-4-8",
            AiProvider.GEMINI to "gemini-3.5-flash",
            AiProvider.OPENAI to "my-custom-translation-model",
        )

        savedModels.forEach { (provider, model) ->
            val legacyConfig = LegacyAiTranslationConfig(
                provider = provider,
                endpoint = "https://api.example.com",
                apiKey = "secret",
                model = model,
                extraBody = "{\"custom_option\":true}",
            )
            val expected = AiTranslationConfig(
                provider = provider,
                endpoint = legacyConfig.endpoint,
                apiKey = legacyConfig.apiKey,
                model = model,
                extraBody = legacyConfig.extraBody,
            )

            assertEquals(
                expected,
                json.decodeFromString<AiTranslationConfig>(
                    legacyJson.encodeToString(LegacyAiTranslationConfig.serializer(), legacyConfig)
                ),
            )
        }
    }

    @Serializable
    private data class LegacyAiTranslationConfig(
        val provider: AiProvider = AiProvider.OPENAI,
        val endpoint: String = AiTranslationConfig.defaultEndpoint(AiProvider.OPENAI),
        val apiKey: String = "",
        val model: String = "gpt-5.4-mini",
        val responseApi: Boolean = false,
        val extraBody: String = "",
    )
}
